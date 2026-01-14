#!/bin/bash
set -a
source .env
set +a

docker-compose up -d --build

if [ -f ngrok.pid ]; then
    echo "Ubijam prethodni ngrok (PID=$(cat ngrok.pid))"
    kill $(cat ngrok.pid) || true
    rm ngrok.pid
fi

echo "Pokrećem ngrok..."
NGROK_PID=$(nohup ngrok http 8080 > ngrok.log 2>&1 & echo $!)
echo "Ngrok PID: $NGROK_PID"

sleep 5

NGROK_URL=$(curl -s http://127.0.0.1:4040/api/tunnels | jq -r '.tunnels[0].public_url')
echo "Ngrok URL: $NGROK_URL"

curl -s -X POST "https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/setWebhook?url=$NGROK_URL/telegram/webhook&secret_token=$TELEGRAM_SECRET_TOKEN"

printf "Webhook postavljen na %s/telegram/webhook\n" "$NGROK_URL"