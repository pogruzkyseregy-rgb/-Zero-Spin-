"""
Друзья и личные сообщения Zero Spin. Без веба: его проверяют тесты (test_zs.py), вызывает zs_server.py.
Писать в личку можно только друзьям: сначала заявка, потом переписка. Так в мессенджер для своих не пишут чужие.
"""
import time
import zlib

import zs_core as core
from zs_core import ZsError

SCHEMA = """
create table if not exists friends(
  a text not null, b text not null, requester text not null, status text not null, created real not null,
  primary key(a, b)
);
create table if not exists dm(
  id integer primary key autoincrement, a text not null, b text not null, sender text not null,
  text text not null default '', image text not null default '', at real not null
);
create index if not exists dm_a on dm(a, id);
create index if not exists dm_b on dm(b, id);
"""

MAX_TEXT = 4000
REQUESTS_PER_HOUR = 30
DM_PER_MINUTE = 40


def init(db) -> None:
    db.executescript(SCHEMA)


def _pair(u1: str, u2: str) -> tuple[str, str]:
    return (u1, u2) if u1 < u2 else (u2, u1)


def _user(db, login: str):
    row = db.execute("select * from users where login_key=?", (core.key(login),)).fetchone()
    if not row:
        raise ZsError(404, "нет такого пользователя")
    return row


def card(row) -> dict:
    """То, что о человеке видят другие: без предупреждений и служебных полей."""
    return {"login": row["login"], "name": row["name"], "status": row["status"], "bio": row["bio"], "role": row["role"],
            "banned": bool(row["banned"]), "has_avatar": bool(row["avatar"]),
            "avatar_v": zlib.crc32(row["avatar"].encode())}


def relation(db, me, other) -> str:
    """self, friends, outgoing (я позвал), incoming (меня позвали) или none."""
    if me["id"] == other["id"]:
        return "self"
    a, b = _pair(me["id"], other["id"])
    r = db.execute("select requester, status from friends where a=? and b=?", (a, b)).fetchone()
    if not r:
        return "none"
    if r["status"] == "friends":
        return "friends"
    return "outgoing" if r["requester"] == me["id"] else "incoming"


def person(db, me, login: str) -> dict:
    other = _user(db, login)
    return card(other) | {"relation": relation(db, me, other)}


def search(db, me, q: str, limit: int = 20) -> list[dict]:
    q = core.key(q)
    if len(q) < 2:
        return []
    like = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
    rows = db.execute("select * from users where login_key like ? escape '\\' and id != ? order by login_key limit ?",
                      (like, me["id"], limit)).fetchall()
    return [card(r) | {"relation": relation(db, me, r)} for r in rows]


def request(db, me, login: str) -> dict:
    other = _user(db, login)
    if other["id"] == me["id"]:
        raise ZsError(400, "себя в друзья не добавить")
    if other["role"] == "aurora":
        raise ZsError(400, "Аврора отвечает в чате поддержки, в друзья её не добавить")
    rel = relation(db, me, other)
    if rel in ("friends", "outgoing"):
        return person(db, me, login)
    if rel == "incoming":
        return accept(db, me, login)
    since = time.time() - 3600
    n = db.execute("select count(*) from friends where requester=? and created>=?", (me["id"], since)).fetchone()[0]
    if n >= REQUESTS_PER_HOUR:
        raise ZsError(429, "слишком много заявок, подожди час")
    a, b = _pair(me["id"], other["id"])
    db.execute("insert into friends(a,b,requester,status,created) values(?,?,?,?,?)", (a, b, me["id"], "pending", time.time()))
    core.event(db, me["id"], me["login"], "friend_request", other["login"])
    return person(db, me, login)


def accept(db, me, login: str) -> dict:
    other = _user(db, login)
    if relation(db, me, other) != "incoming":
        raise ZsError(400, "заявки от этого человека нет")
    a, b = _pair(me["id"], other["id"])
    db.execute("update friends set status='friends' where a=? and b=?", (a, b))
    return person(db, me, login)


def remove(db, me, login: str) -> dict:
    """Отклонить заявку, отменить свою или убрать из друзей. Переписка остаётся, но писать больше нельзя."""
    other = _user(db, login)
    a, b = _pair(me["id"], other["id"])
    db.execute("delete from friends where a=? and b=?", (a, b))
    return person(db, me, login)


def friends(db, me) -> dict:
    rows = db.execute(
        "select u.*, f.requester as f_req, f.status as f_status from friends f join users u on u.id = (case when f.a=? then f.b else f.a end) "
        "where f.a=? or f.b=? order by u.login_key", (me["id"], me["id"], me["id"])).fetchall()
    out = {"friends": [], "incoming": [], "outgoing": []}
    for r in rows:
        if r["f_status"] == "friends":
            out["friends"].append(card(r))
        elif r["f_req"] == me["id"]:
            out["outgoing"].append(card(r))
        else:
            out["incoming"].append(card(r))
    return out


def _msg(db, r, logins: dict) -> dict:
    def login_of(uid):
        if uid not in logins:
            u = db.execute("select login from users where id=?", (uid,)).fetchone()
            logins[uid] = u["login"] if u else "?"
        return logins[uid]
    to = r["b"] if r["sender"] == r["a"] else r["a"]
    return {"id": r["id"], "from": login_of(r["sender"]), "to": login_of(to), "text": r["text"],
            "has_image": bool(r["image"]), "at": r["at"]}


def send(db, me, login: str, text: str, image: str = "") -> dict:
    other = _user(db, login)
    if relation(db, me, other) != "friends":
        raise ZsError(403, "писать можно только друзьям")
    text = (text or "").strip()[:MAX_TEXT]
    if not text and not image:
        raise ZsError(400, "пустое сообщение")
    n = db.execute("select count(*) from dm where sender=? and at>=?", (me["id"], time.time() - 60)).fetchone()[0]
    if n >= DM_PER_MINUTE:
        raise ZsError(429, "слишком часто, подожди минуту")
    a, b = _pair(me["id"], other["id"])
    cur = db.execute("insert into dm(a,b,sender,text,image,at) values(?,?,?,?,?,?)", (a, b, me["id"], text, image, time.time()))
    r = db.execute("select * from dm where id=?", (cur.lastrowid,)).fetchone()
    return _msg(db, r, {me["id"]: me["login"], other["id"]: other["login"]})


def sync(db, me, after: int = 0, limit: int = 300) -> dict:
    """Всё новое для этого человека после сообщения №after: свои и чужие сообщения, число заявок в друзья."""
    rows = db.execute("select * from dm where (a=? or b=?) and id>? order by id limit ?", (me["id"], me["id"], int(after), limit)).fetchall()
    logins = {me["id"]: me["login"]}
    msgs = [_msg(db, r, logins) for r in rows]
    incoming = db.execute("select count(*) from friends where (a=? or b=?) and status='pending' and requester!=?",
                          (me["id"], me["id"], me["id"])).fetchone()[0]
    return {"messages": msgs, "incoming": incoming, "last_id": msgs[-1]["id"] if msgs else int(after), "more": len(rows) == limit}


def dm_image(db, me, msg_id: int) -> str:
    r = db.execute("select * from dm where id=?", (msg_id,)).fetchone()
    if not r or me["id"] not in (r["a"], r["b"]):
        raise ZsError(404, "нет такого сообщения")
    if not r["image"]:
        raise ZsError(404, "нет картинки")
    return r["image"]


def update_me(db, me, name=None, status=None, bio=None, avatar=None) -> dict:
    if name is not None and not name.strip():
        name = me["login"]
    core.update_profile(db, me["login"], name=name, status=status, bio=bio, avatar=avatar)
    return card(_user(db, me["login"]))
