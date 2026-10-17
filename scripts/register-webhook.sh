#!/bin/sh
set -e

NGROK_API="http://ngrok:4040/api/tunnels"
MAX_RETRIES=30
RETRY_INTERVAL=2

echo "Waiting for ngrok tunnel to be ready..."

for i in $(seq 1 $MAX_RETRIES); do
  TUNNELS=$(curl -s "$NGROK_API" 2>/dev/null || echo "")
  if echo "$TUNNELS" | grep -q "public_url"; then
    break
  fi
  echo "  Attempt $i/$MAX_RETRIES - ngrok not ready yet..."
  sleep $RETRY_INTERVAL
done

if ! echo "$TUNNELS" | grep -q "public_url"; then
  echo "ERROR: ngrok tunnel did not become ready after $MAX_RETRIES attempts"
  exit 1
fi

PUBLIC_URL=$(echo "$TUNNELS" | grep -o '"public_url":"https://[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$PUBLIC_URL" ]; then
  echo "ERROR: Could not extract public URL from ngrok"
  exit 1
fi

WEBHOOK_URL="${PUBLIC_URL}/telegram/webhook/${DEFAULT_TENANT_ID}"
echo "Registering webhook: $WEBHOOK_URL"

RESPONSE=$(curl -s -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/setWebhook" \
  -H "Content-Type: application/json" \
  -d "{\"url\": \"${WEBHOOK_URL}\", \"secret_token\": \"${TELEGRAM_SECRET_TOKEN}\"}")

echo "Telegram API response: $RESPONSE"

if echo "$RESPONSE" | grep -q '"ok":true'; then
  echo "Webhook registered successfully!"
  exit 0
else
  echo "ERROR: Failed to register webhook"
  exit 1
fi
