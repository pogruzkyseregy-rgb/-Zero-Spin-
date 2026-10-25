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
apt-get install -y -qq python3-venv caddy curl >/dev/null

echo "[2/6] Код в $DIR"
install -d -o aurora -g aurora "$DIR"
base64 -d <<'PAYLOAD' | tar xzf - -C "$DIR"
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
PAYLOAD
chown -R aurora:aurora "$DIR"

echo "[3/6] Окружение Python"
[ -x "$DIR/venv/bin/python" ] || sudo -u aurora python3 -m venv "$DIR/venv"
sudo -u aurora "$DIR/venv/bin/pip" install -q --upgrade pip
sudo -u aurora "$DIR/venv/bin/pip" install -q fastapi uvicorn "httpx[http2]"

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
