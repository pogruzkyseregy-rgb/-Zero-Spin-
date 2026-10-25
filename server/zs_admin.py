#!/usr/bin/env python3
"""Управление на самом сервере: python3 zs_admin.py users | unban НИК"""
import os, sys
import zs_core as core

db = core.connect(os.environ.get("ZS_DB", os.path.join(os.path.dirname(os.path.abspath(__file__)), "zs.db")))
if len(sys.argv) >= 2 and sys.argv[1] == "users":
    for u in core.users(db):
        print(f"{u['nick']:<24} предупреждений {u['warnings']}  {'БАН: ' + u['ban_reason'] if u['banned'] else ''}")
elif len(sys.argv) >= 3 and sys.argv[1] == "unban":
    print(core.unban(db, sys.argv[2]))
else:
    print(__doc__)
