import os, sqlite3, sys, tempfile
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import zs_core as core

ok = bad = 0
def check(name, cond, info=""):
    global ok, bad
    if cond: ok += 1; print("[ОК]  ", name)
    else: bad += 1; print("[НЕТ] ", name, info)
def err(fn):
    try:
        fn(); return None
    except core.ZsError as e:
        return e.code

T = tempfile.mkdtemp()
db = core.connect(os.path.join(T, "t.db"))
core.ensure_aurora(db)
a = core.register(db, "Vasya_1", "secret12")
check("регистрация по логину и паролю", a["login"] == "Vasya_1" and a["role"] == "user" and len(a["token"]) == 48)
check("логин уникален без учёта регистра", err(lambda: core.register(db, "VASYA_1", "secret12")) == 409)
check("плохой логин", err(lambda: core.register(db, "ab", "secret12")) == 400 and err(lambda: core.register(db, "имя", "secret12")) == 400)
check("короткий пароль", err(lambda: core.register(db, "petya", "123")) == 400)
check("логины команды заняты", err(lambda: core.register(db, "Adonis_AV", "secret12")) == 403 and err(lambda: core.register(db, "evil_av", "secret12")) == 403)
check("в аккаунт Авроры паролем не войти", err(lambda: core.login(db, "Avrora_AV", "")) == 401)
adm = core.create_user(db, "Adonis_AV", "test-pass-1", role="admin", allow_reserved=True, min_password=4)
check("админ создаётся с консоли", adm["role"] == "admin")
check("пароль в базе не хранится открыто", "test-pass-1" not in str(list(db.execute("select pass_hash from users"))))
l = core.login(db, "adonis_av", "test-pass-1")
check("вход админа", l["role"] == "admin" and core.auth(db, l["token"])["login"] == "Adonis_AV")
check("неверный пароль", err(lambda: core.login(db, "Adonis_AV", "wrong-pass")) == 401)
for _ in range(core.LOGIN_TRIES):
    err(lambda: core.login(db, "vasya_1", "wrong"))
check("перебор паролей останавливается", err(lambda: core.login(db, "vasya_1", "secret12")) == 429)
db.execute("delete from login_fail")
row = core.auth(db, a["token"])
s2 = core.login(db, "Vasya_1", "secret12")["token"]
core.logout(db, s2)
check("выход закрывает только свою сессию", err(lambda: core.auth(db, s2)) == 401 and core.auth(db, a["token"])["login"] == "Vasya_1")

calm = lambda h, t, w, u, n: ("Ник меняется в профиле.", False, "")
angry = lambda h, t, w, u, n: ("Граница.", True, "угрожал")
r = core.support(db, row, "как сменить ник", calm)
check("обычный ответ", r["reply"] == "Ник меняется в профиле." and not r["banned"])
r = core.support(db, core.auth(db, a["token"]), "угроза", angry)
check("первая угроза: предупреждение", r["warned"] and r["warnings"] == 1 and not r["banned"])
r = core.support(db, core.auth(db, a["token"]), "угроза", angry)
check("вторая угроза: бан", r["banned"] and r["warnings"] == 2)
check("забаненному она не отвечает", err(lambda: core.support(db, core.auth(db, a["token"]), "привет", calm)) == 403)
check("бан виден в профиле", core.profile(db, "vasya_1")["banned"] is True)
check("снятие бана", core.unban(db, "Vasya_1")["banned"] is False and core.me(core.auth(db, a["token"]))["warnings"] == 0)

p = core.profile(db, "Avrora_AV")
check("профиль Авроры есть сразу", p["role"] == "aurora" and p["name"] == "Аврора" and p["status"])
core.update_profile(db, "Avrora_AV", status="пишу новости", avatar=os.path.join(T, "a.png"))
check("она меняет статус и аватар", core.profile(db, "avrora_av")["status"] == "пишу новости" and core.profile(db, "avrora_av")["has_avatar"])
n1 = core.add_news(db, "Avrora_AV", "Вышла версия с живой поддержкой")
core.add_news(db, "Avrora_AV", "Ещё новость", os.path.join(T, "n.png"))
nl = core.news(db)
check("новости по порядку, свежие сверху", [n["text"] for n in nl] == ["Ещё новость", "Вышла версия с живой поддержкой"] and nl[0]["has_image"] and nl[0]["login"] == "Avrora_AV")
check("удаление новости", core.delete_news(db, n1["id"]) and len(core.news(db)) == 1)

# переезд со старой базы (вход по нику без пароля)
old = os.path.join(T, "old.db")
o = sqlite3.connect(old)
o.executescript("""create table users(id text primary key, nick text unique not null, name text not null default '', token_hash text not null, created real not null,
 warnings integer not null default 0, banned integer not null default 0, banned_at real not null default 0, ban_reason text not null default '');""")
o.execute("insert into users(id,nick,name,token_hash,created) values('u1','oldnick','Старый',?,1)", (core._hash_token("tok"),))
o.commit(); o.close()
db2 = core.connect(old)
check("старый аккаунт переезжает и вход по ключу работает", core.auth(db2, "tok")["login"] == "oldnick")
core.ensure_aurora(db2)
check("на старой базе появляется Аврора", core.profile(db2, "Avrora_AV")["role"] == "aurora")
check("на старой базе работает регистрация", "token" in core.register(db2, "newbie", "secret-1"))
check("на старой базе создаётся админ", core.create_user(db2, "Adonis_AV", "test-pass-1", role="admin", allow_reserved=True, min_password=4)["role"] == "admin")
print(f"\nИТОГ: ок {ok}, нет {bad}")
sys.exit(1 if bad else 0)
