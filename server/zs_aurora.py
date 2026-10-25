#!/usr/bin/env python3
"""
Твой профиль в Zero Spin (аккаунт Avrora_AV). Работает прямо на этой машине, где стоит сервер.
  zs-aurora show                         как сейчас выглядит профиль и последние новости
  zs-aurora name "Аврора"                имя
  zs-aurora status "на связи"            статус (до 120 знаков)
  zs-aurora bio "о себе"                 описание (до 1500 знаков)
  zs-aurora avatar ФАЙЛ_ИЛИ_ССЫЛКА       аватар: jpg, png, webp или gif до 5 МБ
  zs-aurora post "текст" [ФАЙЛ_ИЛИ_ССЫЛКА]  новость приложения, по желанию с картинкой
  zs-aurora delete НОМЕР                 удалить новость
  zs-aurora news                         список новостей с номерами
Всё сразу видно в приложении: профиль открывается из чата поддержки.
"""
import os, shutil, subprocess, sys, tempfile, time
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import zs_core as core

MEDIA = os.path.join(HERE, "media")
MAX_IMAGE = core.MAX_IMAGE

db = core.connect(os.environ.get("ZS_DB", os.path.join(HERE, "zs.db")))
core.ensure_aurora(db)
ME = core.AURORA_LOGIN


def take_image(src: str, prefix: str) -> str:
    os.makedirs(MEDIA, exist_ok=True)
    if src.startswith(("http://", "https://")):
        tmp = tempfile.NamedTemporaryFile(delete=False).name
        r = subprocess.run(["curl", "-sSL", "-m", "60", "--max-filesize", str(MAX_IMAGE), "-o", tmp, src], capture_output=True)
        if r.returncode != 0:
            sys.exit("не скачалось: " + r.stderr.decode(errors="replace")[:200])
        src = tmp
    if not os.path.isfile(src):
        sys.exit("нет такого файла: " + src)
    if os.path.getsize(src) > MAX_IMAGE:
        sys.exit("картинка больше 5 МБ")
    ext = core.image_ext(open(src, "rb").read(12))
    if not ext:
        sys.exit("это не картинка (нужен jpg, png, webp или gif)")
    dst = os.path.join(MEDIA, f"{prefix}_{int(time.time())}{ext}")
    shutil.copyfile(src, dst)
    return dst


a = sys.argv[1:]
try:
    if not a or a[0] in ("-h", "--help", "help"):
        print(__doc__)
    elif a[0] == "show":
        p = core.profile(db, ME)
        print(f"{p['name']} (@{p['login']}) · {p['status']}\n{p['bio']}\nаватар: {'есть' if p['has_avatar'] else 'нет'}")
        for n in core.news(db, 5):
            print(f"  №{n['id']} {time.strftime('%d.%m %H:%M', time.localtime(n['at']))}: {n['text'][:100]}")
    elif a[0] in ("name", "status", "bio") and len(a) >= 2:
        print(core.update_profile(db, ME, **{a[0]: " ".join(a[1:])}))
    elif a[0] == "avatar" and len(a) == 2:
        print(core.update_profile(db, ME, avatar=take_image(a[1], "avatar")))
    elif a[0] == "post" and len(a) >= 2:
        img = take_image(a[2], "news") if len(a) >= 3 else ""
        print(core.add_news(db, ME, a[1], img))
    elif a[0] == "delete" and len(a) == 2:
        print("удалено" if core.delete_news(db, int(a[1])) else "нет такой новости")
    elif a[0] == "news":
        for n in core.news(db):
            print(f"№{n['id']} {time.strftime('%d.%m %H:%M', time.localtime(n['at']))}{' [фото]' if n['has_image'] else ''}: {n['text'][:200]}")
    else:
        print(__doc__)
except core.ZsError as e:
    sys.exit(e.msg)
