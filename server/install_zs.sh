#!/usr/bin/env bash
# Сервер Zero Spin на машине Авроры. Запуск от root НА СЕРВЕРЕ РУКИ:
#   ssh root@37.220.86.132 "bash -s" < install_zs.sh
# Ставит: Python-сервер (служба zs-server под пользователем aurora, код в /home/aurora/zs-server, Аврора его видит)
# и Caddy (HTTPS с бесплатным сертификатом на адрес вида 37-220-86-132.sslip.io). Повторный запуск обновляет код, ключи сохраняет.
set -euo pipefail
[ "$(id -u)" = 0 ] || { echo "Нужен root."; exit 1; }
id aurora >/dev/null 2>&1 || { echo "Нет пользователя aurora"; exit 1; }
DIR=/home/aurora/zs-server
IP="$(ip route get 1.1.1.1 | awk '{for(i=1;i<=NF;i++) if($i=="src") print $(i+1)}')"
HOST="$(echo "$IP" | tr . -).sslip.io"

echo "[1/6] Пакеты"
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq python3-venv caddy >/dev/null

echo "[2/6] Код в $DIR"
install -d -o aurora -g aurora "$DIR"
base64 -d <<'PAYLOAD' | tar xzf - -C "$DIR"
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
wfUKr38ASmAjyABQAAA=
PAYLOAD
chown -R aurora:aurora "$DIR"

echo "[3/6] Окружение Python"
[ -x "$DIR/venv/bin/python" ] || sudo -u aurora python3 -m venv "$DIR/venv"
sudo -u aurora "$DIR/venv/bin/pip" install -q --upgrade pip
sudo -u aurora "$DIR/venv/bin/pip" install -q fastapi uvicorn httpx

echo "[4/6] Ключи"
ENV=/etc/zs-server.env
if [ ! -f "$ENV" ]; then
  cat > "$ENV" <<CONF
RELAY_URL=https://avrora-exe.ru/app-api
PUBLIC_TOKEN=$(openssl rand -hex 24)
ADMIN_TOKEN=$(openssl rand -hex 24)
ZS_DB=$DIR/zs.db
CONF
fi
chmod 640 "$ENV"; chgrp aurora "$ENV"

echo "[5/6] Служба"
cat > /etc/systemd/system/zs-server.service <<UNIT
[Unit]
Description=Zero Spin server
After=network-online.target
[Service]
User=aurora
WorkingDirectory=$DIR
EnvironmentFile=$ENV
ExecStart=$DIR/venv/bin/uvicorn zs_server:app --host 127.0.0.1 --port 8077
Restart=always
RestartSec=3
[Install]
WantedBy=multi-user.target
UNIT
systemctl daemon-reload
systemctl enable --now zs-server >/dev/null 2>&1
systemctl restart zs-server

echo "[6/6] HTTPS (Caddy) для $HOST"
cat > /etc/caddy/Caddyfile <<CADDY
$HOST {
    reverse_proxy 127.0.0.1:8077
}
CADDY
if command -v ufw >/dev/null && ufw status | grep -q active; then ufw allow 80/tcp >/dev/null; ufw allow 443/tcp >/dev/null; fi
systemctl enable --now caddy >/dev/null 2>&1
systemctl reload caddy || systemctl restart caddy
sleep 4
echo
echo "Проверка изнутри: $(curl -s http://127.0.0.1:8077/api/health)"
echo
. "$ENV"
echo "ГОТОВО."
echo "Адрес сервера для приложения:  https://$HOST"
echo "Ключ для основного сервера (одна строка, выполни её там):"
echo "  echo PUBLIC_TOKEN=$PUBLIC_TOKEN >> /opt/aurora-relay/relay.env && systemctl restart aurora-relay"
echo "Админ-ключ (для снятия бана, храни у себя): $ADMIN_TOKEN"
echo "Снять бан прямо здесь: cd $DIR && sudo -u aurora ZS_DB=$DIR/zs.db venv/bin/python zs_admin.py unban НИК"
