"""
Веб-часть сервера Zero Spin. Запуск: uvicorn zs_server:app --host 127.0.0.1 --port 8077
Настройки в zs.env (systemd EnvironmentFile): RELAY_URL, PUBLIC_TOKEN, ADMIN_TOKEN, ZS_DB.
"""
import os, time
from collections import defaultdict

import logging

import httpx
from fastapi import FastAPI, Header, HTTPException, Request
from pydantic import BaseModel

import zs_core as core

RELAY_URL = os.environ.get("RELAY_URL", "https://avrora-exe.ru/app-api").rstrip("/")
PUBLIC_TOKEN = os.environ.get("PUBLIC_TOKEN", "")
ADMIN_TOKEN = os.environ.get("ADMIN_TOKEN", "")
DB = core.connect(os.environ.get("ZS_DB", "zs.db"))

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


class RegisterReq(BaseModel):
    nick: str
    name: str = ""


class SupportReq(BaseModel):
    text: str


class NickReq(BaseModel):
    nick: str


def ask_aurora(hist, text, warnings, uid, nick):
    try:
        r = httpx.post(f"{RELAY_URL}/public", headers={"Authorization": f"Bearer {PUBLIC_TOKEN}"},
                       json={"user": uid, "nick": nick, "messages": hist, "text": text, "warnings": warnings}, timeout=90)
    except httpx.HTTPError as e:
        log.error("relay unreachable: %r", e)
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
    return {"ok": True, "service": "zero-spin", "relay": bool(PUBLIC_TOKEN)}


@app.post("/api/register")
def register(req: RegisterReq, request: Request):
    ip = request.headers.get("x-forwarded-for", request.client.host if request.client else "?").split(",")[0].strip()
    now = time.time()
    _reg_by_ip[ip] = [t for t in _reg_by_ip[ip] if t > now - 3600]
    if len(_reg_by_ip[ip]) >= 5:
        raise HTTPException(429, "слишком много регистраций, попробуй через час")
    out = _wrap(lambda: core.register(DB, req.nick, req.name))
    _reg_by_ip[ip].append(now)
    return out


@app.get("/api/me")
def me(authorization: str = Header(default="")):
    return _wrap(lambda: core.me(core.auth(DB, _bearer(authorization))))


@app.get("/api/users/{nick}")
def user(nick: str):
    return _wrap(lambda: core.public_user(DB, nick))


@app.get("/api/support")
def support_history(authorization: str = Header(default="")):
    row = _wrap(lambda: core.auth(DB, _bearer(authorization)))
    return {"messages": core.history(DB, row["id"]), "me": core.me(row)}


@app.post("/api/support")
def support(req: SupportReq, authorization: str = Header(default="")):
    row = _wrap(lambda: core.auth(DB, _bearer(authorization)))
    return _wrap(lambda: core.support(DB, row, req.text, ask_aurora))


def _admin(h: str):
    if not ADMIN_TOKEN or _bearer(h) != ADMIN_TOKEN:
        raise HTTPException(401, "нет прав")


@app.get("/api/admin/users")
def admin_users(authorization: str = Header(default="")):
    _admin(authorization)
    return {"users": core.users(DB)}


@app.post("/api/admin/unban")
def admin_unban(req: NickReq, authorization: str = Header(default="")):
    _admin(authorization)
    return _wrap(lambda: core.unban(DB, req.nick))
