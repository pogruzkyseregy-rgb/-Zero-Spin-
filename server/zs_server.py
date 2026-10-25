"""
Веб-часть сервера Zero Spin. Запуск: uvicorn zs_server:app --host 127.0.0.1 --port 8077
Настройки в /etc/zs-server.env: RELAY_URL, PUBLIC_TOKEN, ADMIN_TOKEN, ZS_DB.
"""
import base64
import binascii
import json
import logging
import os
import subprocess
import time
from collections import defaultdict
from typing import Optional

import httpx
from fastapi import FastAPI, Header, HTTPException, Request
from fastapi.responses import FileResponse
from pydantic import BaseModel

import zs_core as core
import zs_social as social

RELAY_URL = os.environ.get("RELAY_URL", "https://avrora-exe.ru/app-api").rstrip("/")
PUBLIC_TOKEN = os.environ.get("PUBLIC_TOKEN", "")
ADMIN_TOKEN = os.environ.get("ADMIN_TOKEN", "")
DB_PATH = os.environ.get("ZS_DB", "zs.db")
DB = core.connect(DB_PATH)
MEDIA = os.path.join(os.path.dirname(os.path.abspath(DB_PATH)), "media")
core.ensure_aurora(DB)
social.init(DB)

app = FastAPI(title="Zero Spin")
log = logging.getLogger("uvicorn.error")
_reg_by_ip: dict[str, list[float]] = defaultdict(list)


def _bearer(h: str) -> str:
    return h[7:] if h.startswith("Bearer ") else ""


def _wrap(fn):
    try:
        return fn()
    except core.ZsError as e:
        raise HTTPException(e.code, e.msg)


class AuthReq(BaseModel):
    login: str
    password: str
    privacy: str = ""   # редакция политики, которую человек принял при регистрации


class PrivacyReq(BaseModel):
    version: str


class SupportReq(BaseModel):
    text: str


class ImageReq(BaseModel):
    image: str          # картинка в base64


class LoginReq(BaseModel):
    login: str


class DmReq(BaseModel):
    text: str = ""
    image: str = ""     # фото в base64, по желанию


class PasswordReq(BaseModel):
    password: str


class ProfileReq(BaseModel):
    name: Optional[str] = None
    status: Optional[str] = None
    bio: Optional[str] = None
    cosmetic: Optional[str] = None


class NickReq(BaseModel):
    nick: str = ""
    login: str = ""


class _Resp:
    def __init__(self, status_code: int, text: str):
        self.status_code = status_code
        self.text = text

    def json(self):
        return json.loads(self.text)


def _post_curl(url: str, payload: dict) -> "_Resp":
    """Запасной путь: тот же запрос через curl (он проходит там, где соединения Python обрываются по дороге)."""
    p = subprocess.run(
        ["curl", "-sS", "-m", "90", "-X", "POST", url,
         "-H", f"Authorization: Bearer {PUBLIC_TOKEN}", "-H", "Content-Type: application/json",
         "--data-binary", "@-", "-w", "\n%{http_code}"],
        input=json.dumps(payload, ensure_ascii=False).encode("utf-8"), capture_output=True, timeout=100)
    if p.returncode != 0:
        raise httpx.ConnectError(p.stderr.decode(errors="replace")[:300])
    body, _, code = p.stdout.decode("utf-8", errors="replace").rpartition("\n")
    return _Resp(int(code or 0), body)


# HTTP/2 вместе с HTTP/1.1, как у curl: соединения «только HTTP/1.1» между серверами по дороге рвутся
_client = httpx.Client(http2=True, timeout=90)


_GUIDE_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "zs_guide.md")
_guide_cache = {"mtime": 0.0, "text": ""}


def guide() -> str:
    """Справочник по Zero Spin для Авроры в поддержке. Лежит рядом с сервером и обновляется вместе с ним."""
    try:
        m = os.path.getmtime(_GUIDE_PATH)
        if m != _guide_cache["mtime"]:
            with open(_GUIDE_PATH, encoding="utf-8") as f:
                _guide_cache.update(mtime=m, text=f.read())
    except OSError:
        return ""
    return _guide_cache["text"]


def ask_aurora(hist, text, warnings, uid, nick, role="user"):
    url = f"{RELAY_URL}/public"
    # role приходит из базы этого сервера; relay доверяет ему, потому что запрос подписан PUBLIC_TOKEN
    payload = {"user": uid, "nick": nick, "messages": hist, "text": text, "warnings": warnings, "role": role, "guide": guide()}
    try:
        r = _client.post(url, headers={"Authorization": f"Bearer {PUBLIC_TOKEN}"}, json=payload)
    except httpx.HTTPError as e:
        log.warning("relay via httpx failed (%r), trying curl", e)
        try:
            r = _post_curl(url, payload)
        except (httpx.HTTPError, subprocess.SubprocessError, OSError, ValueError) as e2:
            log.error("relay unreachable: %r", e2)
            raise core.ZsError(502, "Аврора сейчас недоступна (нет связи с её сервером)")
    if r.status_code == 401:
        log.error("relay 401: PUBLIC_TOKEN не совпадает с relay.env")
        raise core.ZsError(502, "Аврора сейчас недоступна (ключ сервера не принят)")
    if r.status_code == 404:
        log.error("relay 404: на основном сервере старый relay.py без /public")
        raise core.ZsError(502, "Аврора сейчас недоступна (её сервер не обновлён)")
    if r.status_code != 200:
        log.error("relay %s: %s", r.status_code, r.text[:300])
        raise core.ZsError(502, "Аврора сейчас не отвечает, попробуй позже")
    try:
        j = r.json()
    except ValueError:
        raise core.ZsError(502, "Аврора ответила непонятно, попробуй ещё раз")
    return j.get("reply", "…"), bool(j.get("flag")), j.get("reason", "")


@app.get("/api/health")
def health():
    return {"ok": True, "service": "zero-spin", "relay": bool(PUBLIC_TOKEN), "auth": "login"}


@app.post("/api/register")
def register(req: AuthReq, request: Request):
    ip = request.headers.get("x-forwarded-for", request.client.host if request.client else "?").split(",")[0].strip()
    now = time.time()
    _reg_by_ip[ip] = [t for t in _reg_by_ip[ip] if t > now - 3600]
    if len(_reg_by_ip[ip]) >= 5:
        raise HTTPException(429, "слишком много регистраций, попробуй через час")
    out = _wrap(lambda: core.register(DB, req.login, req.password, req.privacy))
    _reg_by_ip[ip].append(now)
    return out


@app.post("/api/login")
def login(req: AuthReq):
    return _wrap(lambda: core.login(DB, req.login, req.password))


@app.post("/api/logout")
def logout(authorization: str = Header(default="")):
    core.logout(DB, _bearer(authorization))
    return {"ok": True}


@app.get("/api/me")
def me(authorization: str = Header(default="")):
    return _wrap(lambda: core.me(core.auth(DB, _bearer(authorization))))


@app.get("/api/users/{login}")
def user(login: str):
    return _wrap(lambda: core.profile(DB, login))


@app.get("/api/users/{login}/avatar")
def user_avatar(login: str):
    path = _wrap(lambda: core.avatar_path(DB, login))
    if not os.path.exists(path):
        raise HTTPException(404, "нет аватара")
    return FileResponse(path, headers={"Cache-Control": "public, max-age=300"})


@app.get("/api/news")
def news_list():
    return {"news": core.news(DB)}


@app.get("/api/news/{news_id}/image")
def news_image(news_id: int):
    path = _wrap(lambda: core.news_image(DB, news_id))
    if not os.path.exists(path):
        raise HTTPException(404, "нет картинки")
    return FileResponse(path, headers={"Cache-Control": "public, max-age=3600"})


@app.get("/api/support")
def support_history(authorization: str = Header(default="")):
    row = _wrap(lambda: core.auth(DB, _bearer(authorization)))
    return {"messages": core.history(DB, row["id"]), "me": core.me(row)}


@app.post("/api/support")
def support(req: SupportReq, authorization: str = Header(default="")):
    row = _wrap(lambda: core.auth(DB, _bearer(authorization)))
    return _wrap(lambda: core.support(DB, row, req.text, ask_aurora))


def _admin(h: str):
    """Админ: ключ ADMIN_TOKEN или вход под аккаунтом с ролью admin."""
    t = _bearer(h)
    if ADMIN_TOKEN and t == ADMIN_TOKEN:
        return
    try:
        if core.auth(DB, t)["role"] == "admin":
            return
    except core.ZsError:
        pass
    raise HTTPException(401, "нет прав")


@app.get("/api/admin/users")
def admin_users(authorization: str = Header(default="")):
    _admin(authorization)
    return {"users": core.users(DB)}


@app.post("/api/admin/unban")
def admin_unban(req: NickReq, authorization: str = Header(default="")):
    _admin(authorization)
    return _wrap(lambda: core.unban(DB, req.login or req.nick))


@app.post("/api/admin/aurora-avatar")
def admin_aurora_avatar(req: ImageReq, authorization: str = Header(default="")):
    """Команда «!аватар» из чата с Авророй: админ присылает фото, оно становится её аватаром."""
    row = _wrap(lambda: core.auth(DB, _bearer(authorization)))
    if row["role"] != "admin":
        raise HTTPException(403, "только для админа")
    if len(req.image) > core.MAX_IMAGE * 4 // 3 + 16:
        raise HTTPException(413, "картинка больше 5 МБ")
    try:
        data = base64.b64decode(req.image, validate=True)
    except (binascii.Error, ValueError):
        raise HTTPException(400, "картинка не прочиталась")
    path = _wrap(lambda: core.save_image(data, MEDIA, "avatar"))
    core.event(DB, row["id"], row["login"], "aurora_avatar", os.path.basename(path))
    return _wrap(lambda: core.update_profile(DB, core.AURORA_LOGIN, avatar=path))


# ---------- друзья, личные сообщения, свой профиль ----------

def _me(authorization: str):
    return _wrap(lambda: core.auth(DB, _bearer(authorization)))


def _image_bytes(b64: str) -> bytes:
    if len(b64) > core.MAX_IMAGE * 4 // 3 + 16:
        raise HTTPException(413, "картинка больше 5 МБ")
    try:
        return base64.b64decode(b64, validate=True)
    except (binascii.Error, ValueError):
        raise HTTPException(400, "картинка не прочиталась")


@app.get("/api/people/search")
def people_search(q: str = "", authorization: str = Header(default="")):
    return {"people": social.search(DB, _me(authorization), q)}


@app.get("/api/people/{login}")
def people_one(login: str, authorization: str = Header(default="")):
    me = _me(authorization)
    return _wrap(lambda: social.person(DB, me, login))


@app.get("/api/friends")
def friends_list(authorization: str = Header(default="")):
    return social.friends(DB, _me(authorization))


@app.post("/api/friends/request")
def friends_request(req: LoginReq, authorization: str = Header(default="")):
    me = _me(authorization)
    return _wrap(lambda: social.request(DB, me, req.login))


@app.post("/api/friends/accept")
def friends_accept(req: LoginReq, authorization: str = Header(default="")):
    me = _me(authorization)
    return _wrap(lambda: social.accept(DB, me, req.login))


@app.post("/api/friends/remove")
def friends_remove(req: LoginReq, authorization: str = Header(default="")):
    me = _me(authorization)
    return _wrap(lambda: social.remove(DB, me, req.login))


@app.post("/api/dm/{login}")
def dm_send(login: str, req: DmReq, authorization: str = Header(default="")):
    me = _me(authorization)
    image = ""
    if req.image:
        # сначала права, потом файл: чужой не забьёт диск картинками
        if _wrap(lambda: social.person(DB, me, login))["relation"] != "friends":
            raise HTTPException(403, "писать можно только друзьям")
        image = _wrap(lambda: core.save_image(_image_bytes(req.image), os.path.join(MEDIA, "dm"), "dm"))
    return _wrap(lambda: social.send(DB, me, login, req.text, image))


@app.get("/api/sync")
def sync(after: int = 0, authorization: str = Header(default="")):
    return social.sync(DB, _me(authorization), after)


@app.get("/api/dm-image/{msg_id}")
def dm_image(msg_id: int, authorization: str = Header(default="")):
    me = _me(authorization)
    path = _wrap(lambda: social.dm_image(DB, me, msg_id))
    if not os.path.exists(path):
        raise HTTPException(404, "нет картинки")
    return FileResponse(path, headers={"Cache-Control": "private, max-age=86400"})


@app.post("/api/me/profile")
def me_profile(req: ProfileReq, authorization: str = Header(default="")):
    me = _me(authorization)
    return _wrap(lambda: social.update_me(DB, me, name=req.name, status=req.status, bio=req.bio, cosmetic=req.cosmetic))


@app.post("/api/me/avatar")
def me_avatar(req: ImageReq, authorization: str = Header(default="")):
    me = _me(authorization)
    path = _wrap(lambda: core.save_image(_image_bytes(req.image), MEDIA, "avatar"))
    return _wrap(lambda: social.update_me(DB, me, avatar=path))


@app.post("/api/me/delete")
def me_delete(req: PasswordReq, authorization: str = Header(default="")):
    """Удаление аккаунта: нужен пароль, чтобы чужой с телефона в руках не стёр всё одним нажатием."""
    me = _me(authorization)
    return _wrap(lambda: social.delete_account(DB, me, req.password))


@app.post("/api/me/privacy")
def me_privacy(req: PrivacyReq, authorization: str = Header(default="")):
    me = _me(authorization)
    _wrap(lambda: core.accept_privacy(DB, me["id"], req.version))
    return {"ok": True, "privacy_version": req.version.strip()[:40]}


@app.get("/api/people/{login}/friends")
def people_friends(login: str, authorization: str = Header(default="")):
    _me(authorization)
    return {"friends": _wrap(lambda: social.friends_of(DB, login))}
