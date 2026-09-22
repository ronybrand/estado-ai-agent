#!/usr/bin/env bash
# Rolling swap sem downtime - mesmo padrao do repo estado (ver deploy/estado/deploy.sh la).
# Roda via estado-ai-agent-deploy.timer a cada 5 minutos, ou manualmente:
# ~/estado-ai-agent/deploy.sh
set -euo pipefail
cd "$(dirname "$0")"

IMAGE="ghcr.io/ronybrand/estado-ai-agent:latest"
CURRENT="estado-ai-agent-app"
NEXT="estado-ai-agent-app-next"

docker pull "$IMAGE" >/dev/null

CURRENT_ID="$(docker inspect --format '{{.Image}}' "$CURRENT" 2>/dev/null || echo '')"
NEW_ID="$(docker inspect --format '{{.Id}}' "$IMAGE")"

if [ "$CURRENT_ID" = "$NEW_ID" ]; then
    echo "Imagem sem mudanca, nada a fazer."
    exit 0
fi

echo "Nova imagem detectada, subindo container novo ($NEXT)..."

docker rm -f "$NEXT" >/dev/null 2>&1 || true

# --env-file (nao -e por variavel): este app so tem um env_file simples
# (.env), sem segredos vindos de fontes distintas como o estado (que
# precisa escapar $ no BCrypt hash, ver deploy/estado/lib-swap.sh la) -
# nenhum valor aqui tem "$" no meio, entao --env-file e seguro e evita
# ter que listar cada variavel manualmente (ASK_API_KEY, GEMINI_API_KEY,
# ASK_CORS_ALLOWED_ORIGINS, etc.) toda vez que uma nova for adicionada.
docker run -d --name "$NEXT" \
    --restart unless-stopped \
    --network estado_internal \
    --env-file .env \
    -e ESTADO_API_BASE_URL="http://estado-app:8080" \
    "$IMAGE" >/dev/null

if docker run --rm --network estado_internal curlimages/curl:8.11.1 sh -c "
    for i in \$(seq 1 30); do
        curl -sf http://${NEXT}:8080/actuator/health >/dev/null 2>&1 && exit 0
        sleep 2
    done
    exit 1
"; then
    docker rm -f "$CURRENT" >/dev/null 2>&1 || true
    docker rename "$NEXT" "$CURRENT"
    echo "Deploy concluido sem downtime: $CURRENT agora roda $IMAGE ($NEW_ID)"
else
    echo "Health check falhou, mantendo versao anterior no ar." >&2
    docker logs "$NEXT" --tail 50 2>&1 || true
    docker rm -f "$NEXT" >/dev/null 2>&1 || true
    exit 1
fi
