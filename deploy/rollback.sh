#!/usr/bin/env bash
# Rollback manual pra uma versao anterior conhecida-boa. Mesmo padrao do
# repo estado (ver deploy/estado/rollback.sh la). Reusa o mesmo swap com
# health check do deploy.sh: se a tag de rollback tambem nao ficar
# saudavel, o container atual continua no ar.
#
# Uso:
#   ./rollback.sh          # usa a tag gravada em last-good-tag pelo ultimo deploy.sh
#   ./rollback.sh <sha>    # usa uma tag/sha especifica ja publicada no GHCR
set -euo pipefail
cd "$(dirname "$0")"
set -a
source .env
set +a
source ./lib-swap.sh

CURRENT="estado-ai-agent-app"
NEXT="estado-ai-agent-app-next"

TAG="${1:-}"
if [ -z "$TAG" ]; then
    if [ ! -f last-good-tag ]; then
        echo "Nenhuma tag informada e last-good-tag nao existe ainda (precisa de pelo menos um deploy.sh bem-sucedido antes)." >&2
        echo "Uso: ./rollback.sh <sha-da-tag-no-ghcr>" >&2
        exit 1
    fi
    TAG="$(cat last-good-tag)"
fi

IMAGE="ghcr.io/ronybrand/estado-ai-agent:${TAG}"
echo "Rollback para $IMAGE"
docker pull "$IMAGE" >/dev/null

if swap_to "$IMAGE"; then
    promote
    echo "Rollback concluido: $CURRENT agora roda $IMAGE"
    annotate_deploy "Rollback: estado-ai-agent-app -> ${TAG}" '["deploy","estado-ai-agent","rollback"]'
else
    echo "Rollback abortado: $IMAGE tambem nao ficou saudavel. Investigar manualmente antes de tentar outra tag." >&2
    exit 1
fi
