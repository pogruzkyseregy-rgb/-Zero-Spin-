import os, sys, tempfile
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import zs_core as core

ok = bad = 0
def check(name, cond, info=""):
    global ok, bad
    if cond: ok += 1; print("[ОК]  ", name)
    else: bad += 1; print("[НЕТ] ", name, info)

db = core.connect(os.path.join(tempfile.mkdtemp(), "t.db"))
a = core.register(db, "@Vasya_1", "Вася")
check("регистрация: ник в нижнем регистре без @", a["nick"] == "vasya_1" and len(a["token"]) == 48)
for bad_nick in ["ab", "имя", "a b c", "x" * 30]:
    try:
        core.register(db, bad_nick); check("плохой ник отклонён: " + bad_nick, False)
    except core.ZsError as e:
        check("плохой ник отклонён: " + bad_nick, e.code == 400)
try:
    core.register(db, "vasya_1"); check("занятый ник", False)
except core.ZsError as e:
    check("занятый ник", e.code == 409)
row = core.auth(db, a["token"])
check("вход по токену", row["nick"] == "vasya_1")
try:
    core.auth(db, "fake"); check("чужой токен", False)
except core.ZsError as e:
    check("чужой токен", e.code == 401)

seen = []
def calm(hist, text, w, uid, nick):
    seen.append((hist, text, w)); return ("Ник меняется в профиле.", False, "")
def angry(hist, text, w, uid, nick):
    seen.append((hist, text, w)); return ("Граница. Ещё раз — и разговор окончен.", True, "угрожал")

r = core.support(db, row, "как сменить ник", calm)
check("обычный ответ", r["reply"] == "Ник меняется в профиле." and r["warnings"] == 0 and not r["banned"])
r = core.support(db, core.auth(db, a["token"]), "я тебя найду", angry)
check("первая угроза: предупреждение", r["warned"] and r["warnings"] == 1 and not r["banned"])
check("история уходит ей", seen[-1][0][-1] == {"role": "assistant", "content": "Ник меняется в профиле."} and seen[-1][2] == 0)
r = core.support(db, core.auth(db, a["token"]), "ещё раз угрожаю", angry)
check("вторая угроза: бан", r["banned"] and r["warnings"] == 2)
try:
    core.support(db, core.auth(db, a["token"]), "привет", calm); check("забаненному она не отвечает", False)
except core.ZsError as e:
    check("забаненному она не отвечает", e.code == 403)
check("бан виден другим", core.public_user(db, "@VASYA_1") == {"nick": "vasya_1", "banned": True})
check("me показывает бан и причину", core.me(core.auth(db, a["token"]))["banned"] and core.me(core.auth(db, a["token"]))["ban_reason"] == "угрожал")
check("история целиком", [h["role"] for h in core.history(db, row["id"])] == ["user", "assistant"] * 3)
check("снятие бана", core.unban(db, "vasya_1")["banned"] is False and core.me(core.auth(db, a["token"]))["warnings"] == 0)
b = core.register(db, "spammer")
rb = core.auth(db, b["token"])
for i in range(core.MSG_LIMIT):
    core.support(db, rb, f"m{i}", calm)
try:
    core.support(db, rb, "ещё", calm); check("лимит частоты", False)
except core.ZsError as e:
    check("лимит частоты", e.code == 429)
check("события записаны", [e["kind"] for e in db.execute("select kind from events where nick='vasya_1'")] == ["register", "warn", "ban", "unban"])
print(f"\nИТОГ: ок {ok}, нет {bad}")
sys.exit(1 if bad else 0)
