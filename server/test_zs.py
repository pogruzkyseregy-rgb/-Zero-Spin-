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

calm = lambda h, t, w, u, n, r="user": ("Ник меняется в профиле.", False, "")
angry = lambda h, t, w, u, n, r="user": ("Граница.", True, "угрожал")
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
# админа она узнаёт: роль уходит в вызов, предупреждений ему не бывает
seen = {}
def spy(h, t, w, u, n, r="user"):
    seen["role"] = r
    return ("Привет, Адонис.", True, "показалось")

ra = core.support(db, core.auth(db, core.login(db, "adonis_av", "test-pass-1")["token"]), "привет", spy)
check("Аврора получает роль админа", seen.get("role") == "admin")
check("админу не ставят предупреждение", not ra["warned"] and ra["warnings"] == 0 and not ra["banned"])
png = b"\x89PNG\r\n\x1a\n" + b"0" * 20
pth = core.save_image(png, os.path.join(T, "media"), "avatar")
check("картинка сохраняется", pth.endswith(".png") and os.path.getsize(pth) == len(png))
check("не картинку не принимает", err(lambda: core.save_image(b"hello world!", os.path.join(T, "media"), "x")) == 400)

core.ensure_aurora(db2)
check("на старой базе появляется Аврора", core.profile(db2, "Avrora_AV")["role"] == "aurora")
check("на старой базе работает регистрация", "token" in core.register(db2, "newbie", "secret-1"))
check("на старой базе создаётся админ", core.create_user(db2, "Adonis_AV", "test-pass-1", role="admin", allow_reserved=True, min_password=4)["role"] == "admin")

# ---------- друзья и личка ----------
import zs_social as social
social.init(db)
def U(login):
    t = core.register(db, login, "secret-1")["token"]
    return core.auth(db, t)
ann, bob, eve = U("anna_k"), U("bob_r"), U("eve_x")
check("поиск по началу логина", [p["login"] for p in social.search(db, ann, "bo")] == ["bob_r"])
check("поиск без себя и без коротких запросов", social.search(db, ann, "a") == [] and all(p["login"] != "anna_k" for p in social.search(db, ann, "an")))
check("поиск не ломается на % и _", social.search(db, ann, "%%") == [] and [p["login"] for p in social.search(db, ann, "bob_")] == ["bob_r"])
check("писать чужим нельзя", err(lambda: social.send(db, ann, "bob_r", "привет")) == 403)
check("заявка", social.request(db, ann, "bob_r")["relation"] == "outgoing" and social.person(db, bob, "anna_k")["relation"] == "incoming")
check("заявка видна в sync", social.sync(db, bob)["incoming"] == 1)
check("встречная заявка = дружба", social.request(db, bob, "anna_k")["relation"] == "friends")
social.update_me(db, bob, status="на смене")
check("в списке друзей, со своим статусом", [(f["login"], f["status"]) for f in social.friends(db, ann)["friends"]] == [("bob_r", "на смене")])
check("себя не добавить", err(lambda: social.request(db, ann, "anna_k")) == 400)
check("Аврора принимает заявку сразу", social.request(db, ann, "Avrora_AV")["relation"] == "friends" and "Avrora_AV" in [f["login"] for f in social.friends(db, ann)["friends"]])
check("Авроре в личку нельзя, только в поддержку", err(lambda: social.send(db, ann, "Avrora_AV", "привет")) == 400)
social.remove(db, ann, "Avrora_AV")
check("украшение аватара", social.update_me(db, ann, cosmetic="crown")["cosmetic"] == "crown" and core.profile(db, "anna_k")["cosmetic"] == "crown")
check("ушки нельзя: они только у Авроры", err(lambda: social.update_me(db, ann, cosmetic="ears")) == 400)
check("снять украшение", social.update_me(db, ann, cosmetic="")["cosmetic"] == "")
m1 = social.send(db, ann, "Bob_R", "привет, Боб")
m2 = social.send(db, bob, "anna_k", "привет!")
sa = social.sync(db, ann)
check("личка в обе стороны", [(m["from"], m["to"], m["text"]) for m in sa["messages"]] == [("anna_k", "bob_r", "привет, Боб"), ("bob_r", "anna_k", "привет!")])
check("sync с номера отдаёт только новое", [m["id"] for m in social.sync(db, ann, m1["id"])["messages"]] == [m2["id"]])
check("чужую переписку не видно", social.sync(db, eve)["messages"] == [])
pimg = core.save_image(b"\xff\xd8\xff" + b"0" * 20, os.path.join(T, "media", "dm"), "dm")
m3 = social.send(db, ann, "bob_r", "", pimg)
check("фото в личке", m3["has_image"] and social.dm_image(db, bob, m3["id"]) == pimg)
check("чужое фото не отдаётся", err(lambda: social.dm_image(db, eve, m3["id"])) == 404)
check("пустое сообщение нельзя", err(lambda: social.send(db, ann, "bob_r", "   ")) == 400)
check("в карточке нет предупреждений", "warnings" not in social.person(db, eve, "anna_k"))
check("свой профиль", social.update_me(db, ann, name="Анна", status="тут", bio="о себе")["name"] == "Анна" and core.profile(db, "anna_k")["status"] == "тут")
check("пустое имя = логин", social.update_me(db, ann, name="  ")["name"] == "anna_k")
check("убрать из друзей", social.remove(db, bob, "anna_k")["relation"] == "none" and err(lambda: social.send(db, ann, "bob_r", "эй")) == 403)
social.request(db, eve, "anna_k")
banned_u = U("bad_guy")
db.execute("update users set banned=1 where id=?", (banned_u["id"],))
check("забаненного Аврора в друзья не берёт", err(lambda: social.request(db, core.auth(db, core.login(db, "bad_guy", "secret-1")["token"]), "Avrora_AV")) == 403)
check("отклонить заявку", social.remove(db, ann, "eve_x")["relation"] == "none" and social.sync(db, ann)["incoming"] == 0)

# удаление аккаунта
gone = U("gone_soon")
social.request(db, gone, "bob_r"); social.request(db, bob, "gone_soon")
gimg = core.save_image(b"\xff\xd8\xff" + b"1" * 20, os.path.join(T, "media", "dm"), "dm")
social.send(db, gone, "bob_r", "пока", gimg)
check("без пароля аккаунт не удалить", err(lambda: social.delete_account(db, gone, "wrong")) == 401)
r = social.delete_account(db, gone, "secret-1")
check("аккаунт удаляется вместе с перепиской и фото", r["deleted"] and not os.path.exists(gimg)
      and db.execute("select count(*) from users where login_key='gone_soon'").fetchone()[0] == 0
      and all(m["from"] != "gone_soon" for m in social.sync(db, bob)["messages"])
      and "gone_soon" not in [f["login"] for f in social.friends(db, bob)["friends"]])
check("логин после удаления снова свободен", "token" in core.register(db, "gone_soon", "secret-2"))
check("аккаунт Авроры не удалить", err(lambda: social.delete_account(db, db.execute("select * from users where login_key='avrora_av'").fetchone(), "x")) == 403)

# украшения в приложении и на сервере совпадают (проверка только в репозитории, где есть код приложения)
kt = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "java", "com", "zerospin", "app", "ui", "hud", "Cosmetics.kt")
if os.path.exists(kt):
    import re as _re
    src = open(kt, encoding="utf-8").read()
    block = src[src.index("AvatarCosmetics"):src.index("BackCosmetics")]
    ids = _re.findall(r'"([a-z0-9]*)" to "', block)
    check("украшения в приложении и на сервере одни и те же", tuple(ids) == core.COSMETICS, ids)

print(f"\nИТОГ: ок {ok}, нет {bad}")
sys.exit(1 if bad else 0)
