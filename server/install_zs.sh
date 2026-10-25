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
