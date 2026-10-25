"""
Сервер Zero Spin: ядро без веба. База SQLite, вход по логину и паролю, роли, поддержка с Авророй,
предупреждения и бан, профили и новости. Его же проверяют тесты (test_zs.py).
Предупреждения и бан хранятся здесь, на сервере: переустановка приложения их не сбрасывает.
"""
import hashlib, hmac, os, re, secrets, sqlite3, time, zlib

LOGIN_RE = re.compile(r"^[A-Za-z0-9_]{3,24}$")
RESERVED_SUFFIX = "_av"            # логины команды: Adonis_AV, Avrora_AV; обычный человек их не займёт
AURORA_LOGIN = "Avrora_AV"
ADMIN_LOGIN = "Adonis_AV"
MIN_PASSWORD = 6                   # для обычных; админ задаёт свой через консоль сервера
WARN_LIMIT = 2                     # второе предупреждение = бан
MSG_LIMIT = 20                     # сообщений Авроре за 10 минут
MSG_WINDOW = 600
LOGIN_TRIES = 8                    # неудачных входов за 10 минут на логин
HISTORY = 12
PBKDF2_ROUNDS = 200_000

SCHEMA = """
create table if not exists users(
  id text primary key, nick text unique, name text not null default '',
  token_hash text not null default '', created real not null,
  warnings integer not null default 0, banned integer not null default 0, banned_at real not null default 0, ban_reason text not null default ''
);
create table if not exists sessions(token_hash text primary key, user_id text not null, created real not null);
create table if not exists support(
  id integer primary key autoincrement, user_id text not null, role text not null, text text not null, at real not null, flag integer not null default 0
);
create index if not exists support_user on support(user_id, id);
create table if not exists news(id integer primary key autoincrement, author_id text not null, text text not null, image text not null default '', at real not null);
create table if not exists events(id integer primary key autoincrement, at real not null, user_id text, nick text, kind text, detail text);
create table if not exists login_fail(login_key text not null, at real not null);
"""

USER_COLUMNS = {
    "login": "text not null default ''", "login_key": "text not null default ''", "pass_hash": "text not null default ''",
    "role": "text not null default 'user'", "status": "text not null default ''", "bio": "text not null default ''",
    "avatar": "text not null default ''", "cosmetic": "text not null default ''",
}

# Украшения аватара для всех. Лисьи ушки сюда не входят: они только у Авроры.
COSMETICS = ("", "halo", "demon", "orbit", "hat", "crown", "stars", "sparkle", "comet", "crown2", "starcrown",
             "thornhalo", "thorns", "eclipse", "radiation", "dna", "claws", "glitch404", "loser")


class ZsError(Exception):
    def __init__(self, code: int, msg: str):
        super().__init__(msg)
        self.code = code
        self.msg = msg


def connect(path: str) -> sqlite3.Connection:
    db = sqlite3.connect(path, check_same_thread=False, isolation_level=None)
    db.row_factory = sqlite3.Row
    db.executescript(SCHEMA)
    have = {r["name"] for r in db.execute("pragma table_info(users)")}
    for col, decl in USER_COLUMNS.items():
        if col not in have:
            db.execute(f"alter table users add column {col} {decl}")
    # старые аккаунты (вход по нику без пароля): логин = ник, их ключи переезжают в sessions
    db.execute("update users set login=nick, login_key=lower(nick) where login='' and nick is not null")
    for r in db.execute("select id, token_hash, created from users where token_hash != ''").fetchall():
        db.execute("insert or ignore into sessions(token_hash,user_id,created) values(?,?,?)", (r["token_hash"], r["id"], r["created"]))
    db.execute("update users set token_hash='' where token_hash != ''")
    db.execute("create unique index if not exists users_login on users(login_key)")
    return db


def _hash_token(token: str) -> str:
    return hashlib.sha256(token.encode()).hexdigest()


def hash_password(pw: str) -> str:
    salt = secrets.token_hex(16)
    h = hashlib.pbkdf2_hmac("sha256", pw.encode(), salt.encode(), PBKDF2_ROUNDS).hex()
    return f"pbkdf2${PBKDF2_ROUNDS}${salt}${h}"


def check_password(pw: str, stored: str) -> bool:
    try:
        _, rounds, salt, h = stored.split("$")
        got = hashlib.pbkdf2_hmac("sha256", pw.encode(), salt.encode(), int(rounds)).hex()
        return hmac.compare_digest(got, h)
    except (ValueError, AttributeError):
        return False


def key(login: str) -> str:
    return login.strip().lstrip("@").lower()


def event(db, uid, nick, kind, detail):
    db.execute("insert into events(at,user_id,nick,kind,detail) values(?,?,?,?,?)", (time.time(), uid, nick, kind, (detail or "")[:300]))


def _new_session(db, uid: str) -> str:
    token = secrets.token_hex(24)
    db.execute("insert into sessions(token_hash,user_id,created) values(?,?,?)", (_hash_token(token), uid, time.time()))
    return token


def create_user(db, login: str, password: str, role: str = "user", name: str = "", allow_reserved: bool = False, min_password: int = MIN_PASSWORD) -> dict:
    login = login.strip().lstrip("@")
    if not LOGIN_RE.match(login):
        raise ZsError(400, "логин: 3–24 символа, латиница, цифры и _")
    k = key(login)
    if k.endswith(RESERVED_SUFFIX) and not allow_reserved:
        raise ZsError(403, "логины с _AV на конце заняты командой Zero Spin")
    if len(password) < min_password:
        raise ZsError(400, f"пароль: не короче {min_password} символов")
    if db.execute("select 1 from users where login_key=?", (k,)).fetchone():
        raise ZsError(409, "такой логин уже занят")
    uid = "u" + secrets.token_hex(8)
    # token_hash остался от старой схемы (там он NOT NULL без значения по умолчанию): пишем пустой
    db.execute("insert into users(id,nick,login,login_key,name,pass_hash,role,created,token_hash) values(?,?,?,?,?,?,?,?,'')",
               (uid, k, login, k, (name or login).strip()[:60], hash_password(password), role, time.time()))
    event(db, uid, login, "register", role)
    return {"user_id": uid, "login": login, "role": role}


def register(db, login: str, password: str) -> dict:
    u = create_user(db, login, password)
    return u | {"token": _new_session(db, u["user_id"])}


def login(db, login_: str, password: str) -> dict:
    k = key(login_)
    since = time.time() - 600
    if db.execute("select count(*) from login_fail where login_key=? and at>=?", (k, since)).fetchone()[0] >= LOGIN_TRIES:
        raise ZsError(429, "слишком много попыток, подожди 10 минут")
    row = db.execute("select * from users where login_key=?", (k,)).fetchone()
    if not row or not row["pass_hash"] or not check_password(password, row["pass_hash"]):
        db.execute("insert into login_fail(login_key,at) values(?,?)", (k, time.time()))
        raise ZsError(401, "неверный логин или пароль")
    db.execute("delete from login_fail where login_key=?", (k,))
    return {"user_id": row["id"], "login": row["login"], "role": row["role"], "token": _new_session(db, row["id"])}


def logout(db, token: str):
    db.execute("delete from sessions where token_hash=?", (_hash_token(token),))


def auth(db, token: str):
    if not token:
        raise ZsError(401, "нет токена")
    row = db.execute("select u.* from sessions s join users u on u.id=s.user_id where s.token_hash=?", (_hash_token(token),)).fetchone()
    if not row:
        raise ZsError(401, "вход устарел, войди заново")
    return row


def me(row) -> dict:
    return {"user_id": row["id"], "login": row["login"], "nick": row["login"], "name": row["name"], "role": row["role"],
            "warnings": row["warnings"], "banned": bool(row["banned"]), "ban_reason": row["ban_reason"], "warn_limit": WARN_LIMIT}


def profile(db, login_: str) -> dict:
    row = db.execute("select * from users where login_key=?", (key(login_),)).fetchone()
    if not row:
        raise ZsError(404, "нет такого пользователя")
    return {"login": row["login"], "name": row["name"], "status": row["status"], "bio": row["bio"], "role": row["role"],
            "banned": bool(row["banned"]), "has_avatar": bool(row["avatar"]), "avatar_v": zlib.crc32(row["avatar"].encode()),
            "cosmetic": row["cosmetic"]}


def avatar_path(db, login_: str) -> str:
    row = db.execute("select avatar from users where login_key=?", (key(login_),)).fetchone()
    if not row or not row["avatar"]:
        raise ZsError(404, "нет аватара")
    return row["avatar"]


def update_profile(db, login_: str, name=None, status=None, bio=None, avatar=None, cosmetic=None) -> dict:
    row = db.execute("select id from users where login_key=?", (key(login_),)).fetchone()
    if not row:
        raise ZsError(404, "нет такого пользователя")
    if cosmetic is not None:
        if cosmetic not in COSMETICS:
            raise ZsError(400, "нет такого украшения")
        db.execute("update users set cosmetic=? where id=?", (cosmetic, row["id"]))
    for col, val, lim in (("name", name, 60), ("status", status, 120), ("bio", bio, 1500), ("avatar", avatar, 500)):
        if val is not None:
            db.execute(f"update users set {col}=? where id=?", (val.strip()[:lim], row["id"]))
    return profile(db, login_)


def history(db, uid: str, limit: int = 200) -> list[dict]:
    rows = db.execute("select role, text, at, flag from support where user_id=? order by id desc limit ?", (uid, limit)).fetchall()
    return [dict(r) | {"flag": bool(r["flag"])} for r in reversed(rows)]


MAX_IMAGE = 5_000_000
IMAGE_MAGIC = {b"\xff\xd8\xff": ".jpg", b"\x89PNG": ".png", b"GIF8": ".gif", b"RIFF": ".webp"}


def image_ext(data: bytes):
    """Расширение по первым байтам файла или None, если это не картинка."""
    return next((e for m, e in IMAGE_MAGIC.items() if data.startswith(m) and (e != ".webp" or data[8:12] == b"WEBP")), None)


def save_image(data: bytes, media: str, prefix: str) -> str:
    if len(data) > MAX_IMAGE:
        raise ZsError(413, "картинка больше 5 МБ")
    ext = image_ext(data[:12])
    if not ext:
        raise ZsError(400, "это не картинка (нужен jpg, png, webp или gif)")
    os.makedirs(media, exist_ok=True)
    path = os.path.join(media, f"{prefix}_{int(time.time() * 1000)}{ext}")
    with open(path, "wb") as f:
        f.write(data)
    return path


def support(db, row, text: str, ask) -> dict:
    """ask(history, text, warnings, user_id, login, role) -> (reply, flag, reason): вызов публичной Авроры.
    Админу (Адонису) предупреждения не ставятся: его она узнаёт и не банит."""
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
    reply, flag, reason = ask(hist, text, row["warnings"], row["id"], row["login"], row["role"])
    if row["role"] == "admin":
        flag, reason = False, ""
    now = time.time()
    db.execute("insert into support(user_id,role,text,at,flag) values(?,?,?,?,?)", (row["id"], "user", text, now, 1 if flag else 0))
    db.execute("insert into support(user_id,role,text,at) values(?,?,?,?)", (row["id"], "assistant", reply, now + 0.001))
    warnings = row["warnings"] + (1 if flag else 0)
    banned = warnings >= WARN_LIMIT
    if flag:
        db.execute("update users set warnings=?, banned=?, banned_at=?, ban_reason=? where id=?",
                   (warnings, 1 if banned else 0, now if banned else 0, reason if banned else row["ban_reason"], row["id"]))
        event(db, row["id"], row["login"], "ban" if banned else "warn", reason)
    return {"reply": reply, "warned": flag, "reason": reason, "warnings": warnings, "banned": banned, "warn_limit": WARN_LIMIT}


def unban(db, login_: str) -> dict:
    cur = db.execute("update users set warnings=0, banned=0, banned_at=0, ban_reason='' where login_key=?", (key(login_),))
    if cur.rowcount == 0:
        raise ZsError(404, "нет такого пользователя")
    event(db, None, login_, "unban", "")
    return {"login": login_, "banned": False}


def users(db) -> list[dict]:
    return [dict(r) for r in db.execute("select login, name, role, warnings, banned, ban_reason, created from users order by created")]


# ---------- новости (их пишет Аврора) ----------

def add_news(db, login_: str, text: str, image: str = "") -> dict:
    row = db.execute("select id, login from users where login_key=?", (key(login_),)).fetchone()
    if not row:
        raise ZsError(404, "нет такого пользователя")
    text = text.strip()
    if not text:
        raise ZsError(400, "пустая новость")
    cur = db.execute("insert into news(author_id,text,image,at) values(?,?,?,?)", (row["id"], text[:4000], image[:500], time.time()))
    return {"id": cur.lastrowid, "text": text[:4000]}


def delete_news(db, news_id: int) -> bool:
    return db.execute("delete from news where id=?", (news_id,)).rowcount > 0


def news(db, limit: int = 50) -> list[dict]:
    rows = db.execute("select n.id, n.text, n.image, n.at, u.login, u.name from news n join users u on u.id=n.author_id order by n.id desc limit ?", (limit,)).fetchall()
    return [{"id": r["id"], "text": r["text"], "has_image": bool(r["image"]), "at": r["at"], "login": r["login"], "name": r["name"]} for r in rows]


def news_image(db, news_id: int) -> str:
    row = db.execute("select image from news where id=?", (news_id,)).fetchone()
    if not row or not row["image"]:
        raise ZsError(404, "нет картинки")
    return row["image"]


def ensure_aurora(db) -> None:
    """Аккаунт Авроры существует всегда; войти в него паролем нельзя: она ведёт профиль с сервера командой zs-aurora."""
    if not db.execute("select 1 from users where login_key=?", (key(AURORA_LOGIN),)).fetchone():
        create_user(db, AURORA_LOGIN, secrets.token_hex(32), role="aurora", name="Аврора", allow_reserved=True)
        db.execute("update users set status=?, bio=? where login_key=?",
                   ("на связи", "ИИ Zero Spin. Поддержка, новости приложения и границы, которые лучше не проверять.", key(AURORA_LOGIN)))
