#!/usr/bin/env python3
"""
Управление на самом сервере (от root или aurora):
  zs-admin users                 все аккаунты, предупреждения, баны
  zs-admin unban ЛОГИН           снять бан
  zs-admin create-admin ЛОГИН    создать админа (пароль спросит, в истории команд его не будет)
  zs-admin passwd ЛОГИН          сменить пароль
"""
import getpass, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import zs_core as core

db = core.connect(os.environ.get("ZS_DB", os.path.join(os.path.dirname(os.path.abspath(__file__)), "zs.db")))
a = sys.argv[1:]


def ask_pw() -> str:
    p1 = getpass.getpass("Пароль: ")
    p2 = getpass.getpass("Ещё раз: ")
    if p1 != p2:
        sys.exit("Пароли не совпали.")
    return p1


try:
    if a[:1] == ["users"]:
        for u in core.users(db):
            print(f"{u['login']:<24} {u['role']:<7} предупреждений {u['warnings']}  {'БАН: ' + u['ban_reason'] if u['banned'] else ''}")
    elif a[:1] == ["unban"] and len(a) == 2:
        print(core.unban(db, a[1]))
    elif a[:1] == ["create-admin"] and len(a) == 2:
        print(core.create_user(db, a[1], ask_pw(), role="admin", allow_reserved=True, min_password=4))
    elif a[:1] == ["passwd"] and len(a) == 2:
        pw = ask_pw()
        if len(pw) < 4:
            sys.exit("Слишком короткий.")
        n = db.execute("update users set pass_hash=? where login_key=?", (core.hash_password(pw), core.key(a[1]))).rowcount
        print("готово" if n else "нет такого логина")
    else:
        print(__doc__)
except core.ZsError as e:
    sys.exit(e.msg)
