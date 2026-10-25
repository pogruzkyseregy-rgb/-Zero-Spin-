"""
Веб-часть сервера Zero Spin. Запуск: uvicorn zs_server:app --host 127.0.0.1 --port 8077
Настройки в /etc/zs-server.env: RELAY_URL, PUBLIC_TOKEN, ADMIN_TOKEN, ZS_DB.
"""
import json
import logging
import os
import subprocess
import time
from collections import defaultdict

import httpx
from fastapi import FastAPI, Header, HTTPException, Request
from fastapi.responses import FileResponse
from pydantic import BaseModel

import zs_core as core

RELAY_URL = os.environ.get("RELAY_URL", "https://avrora-exe.ru/app-api").rstrip("/")
PUBLIC_TOKEN = os.environ.get("PUBLIC_TOKEN", "")
ADMIN_TOKEN = os.environ.get("ADMIN_TOKEN", "")
DB = core.connect(os.environ.get("ZS_DB", "zs.db"))
core.ensure_aurora(DB)

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


class SupportReq(BaseModel):
    text: str


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


def ask_aurora(hist, text, warnings, uid, nick):
    url = f"{RELAY_URL}/public"
    payload = {"user": uid, "nick": nick, "messages": hist, "text": text, "warnings": warnings}
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
    out = _wrap(lambda: core.register(DB, req.login, req.password))
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
