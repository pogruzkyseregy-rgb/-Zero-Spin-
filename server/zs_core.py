"""
Сервер Zero Spin: ядро без веба (база, правила предупреждений и бана). Его же проверяют тесты.
Предупреждения и бан хранятся здесь, на сервере: переустановка приложения их не сбрасывает.
"""
import hashlib, re, secrets, sqlite3, time

NICK_RE = re.compile(r"^[a-z0-9_.]{3,24}$")
WARN_LIMIT = 2                 # второе предупреждение = бан
MSG_LIMIT = 20                 # сообщений Авроре
MSG_WINDOW = 600               # за 10 минут
HISTORY = 12

SCHEMA = """
create table if not exists users(
  id text primary key, nick text unique not null, name text not null default '',
  token_hash text not null, created real not null,
  warnings integer not null default 0, banned integer not null default 0, banned_at real not null default 0, ban_reason text not null default ''
);
create table if not exists support(
  id integer primary key autoincrement, user_id text not null, role text not null, text text not null, at real not null, flag integer not null default 0
);
create index if not exists support_user on support(user_id, id);
create table if not exists events(
  id integer primary key autoincrement, at real not null, user_id text, nick text, kind text, detail text
);
"""


class ZsError(Exception):
    def __init__(self, code: int, msg: str):
        super().__init__(msg)
        self.code = code
        self.msg = msg


def connect(path: str) -> sqlite3.Connection:
    db = sqlite3.connect(path, check_same_thread=False, isolation_level=None)
    db.row_factory = sqlite3.Row
    db.executescript(SCHEMA)
    return db


def _hash(token: str) -> str:
    return hashlib.sha256(token.encode()).hexdigest()


def normalize_nick(nick: str) -> str:
    return nick.strip().lstrip("@").lower()


def register(db, nick: str, name: str = "") -> dict:
    n = normalize_nick(nick)
    if not NICK_RE.match(n):
        raise ZsError(400, "ник: 3–24 символа, латиница, цифры, _ и точка")
    if db.execute("select 1 from users where nick=?", (n,)).fetchone():
        raise ZsError(409, "такой ник уже занят")
    uid = "u" + secrets.token_hex(8)
    token = secrets.token_hex(24)
    db.execute("insert into users(id,nick,name,token_hash,created) values(?,?,?,?,?)", (uid, n, name.strip()[:60], _hash(token), time.time()))
    event(db, uid, n, "register", "")
    return {"user_id": uid, "nick": n, "token": token}


def auth(db, token: str):
    if not token:
        raise ZsError(401, "нет токена")
    row = db.execute("select * from users where token_hash=?", (_hash(token),)).fetchone()
    if not row:
        raise ZsError(401, "неизвестный токен")
    return row


def me(row) -> dict:
    return {"user_id": row["id"], "nick": row["nick"], "name": row["name"], "warnings": row["warnings"],
            "banned": bool(row["banned"]), "ban_reason": row["ban_reason"], "warn_limit": WARN_LIMIT}


def public_user(db, nick: str) -> dict:
    row = db.execute("select nick, banned from users where nick=?", (normalize_nick(nick),)).fetchone()
    if not row:
        raise ZsError(404, "нет такого пользователя")
    return {"nick": row["nick"], "banned": bool(row["banned"])}


def history(db, uid: str, limit: int = 200) -> list[dict]:
    rows = db.execute("select role, text, at, flag from support where user_id=? order by id desc limit ?", (uid, limit)).fetchall()
    return [dict(r) | {"flag": bool(r["flag"])} for r in reversed(rows)]


def event(db, uid, nick, kind, detail):
    db.execute("insert into events(at,user_id,nick,kind,detail) values(?,?,?,?,?)", (time.time(), uid, nick, kind, detail[:300]))


def support(db, row, text: str, ask) -> dict:
    """ask(history, text, warnings, user_id, nick) -> (reply, flag, reason): вызов публичной Авроры."""
    if row["banned"]:
        raise ZsError(403, "Аврора больше не отвечает тебе")
    text = text.strip()
    if not text:
        raise ZsError(400, "пустое сообщение")
    text = text[:2000]
    since = time.time() - MSG_WINDOW
    n = db.execute("select count(*) from support where user_id=? and role='user' and at>=?", (row["id"], since)).fetchone()[0]
    if n >= MSG_LIMIT:
        raise ZsError(429, "слишком часто, подожди несколько минут")
    hist = [{"role": "user" if h["role"] == "user" else "assistant", "content": h["text"]} for h in history(db, row["id"], HISTORY)]
    reply, flag, reason = ask(hist, text, row["warnings"], row["id"], row["nick"])
    now = time.time()
    db.execute("insert into support(user_id,role,text,at,flag) values(?,?,?,?,?)", (row["id"], "user", text, now, 1 if flag else 0))
    db.execute("insert into support(user_id,role,text,at) values(?,?,?,?)", (row["id"], "assistant", reply, now + 0.001))
    warnings = row["warnings"] + (1 if flag else 0)
    banned = warnings >= WARN_LIMIT
    if flag:
        db.execute("update users set warnings=?, banned=?, banned_at=?, ban_reason=? where id=?",
                   (warnings, 1 if banned else 0, now if banned else 0, reason if banned else row["ban_reason"], row["id"]))
        event(db, row["id"], row["nick"], "ban" if banned else "warn", reason)
    return {"reply": reply, "warned": flag, "reason": reason, "warnings": warnings, "banned": banned, "warn_limit": WARN_LIMIT}


def unban(db, nick: str) -> dict:
    n = normalize_nick(nick)
    cur = db.execute("update users set warnings=0, banned=0, banned_at=0, ban_reason='' where nick=?", (n,))
    if cur.rowcount == 0:
        raise ZsError(404, "нет такого пользователя")
    event(db, None, n, "unban", "")
    return {"nick": n, "banned": False}


def users(db) -> list[dict]:
    return [dict(r) for r in db.execute("select nick, name, warnings, banned, ban_reason, created from users order by created")]
