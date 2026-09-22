# Sourced por deploy.sh e rollback.sh - sobe uma imagem com nome temporario,
# espera /actuator/health, so entao deixa o chamador substituir o container
# atual. Sem shebang de proposito: nunca e executado diretamente. Mesmo
# padrao do repo estado (ver deploy/estado/lib-swap.sh la), simplificado
# pra um container so (sem Postgres/rede dupla).
#
# Requer no ambiente: CURRENT, NEXT (setados pelo chamador). O .env deste
# app nao tem nenhum valor com "$" no meio (diferente do estado, que
# precisa escapar o hash BCrypt), entao --env-file e seguro aqui - nao
# precisa listar cada variavel manualmente.

swap_to() {
    local image="$1"

    docker rm -f "$NEXT" >/dev/null 2>&1 || true

    docker run -d --name "$NEXT" \
        --restart unless-stopped \
        --network estado_internal \
        --env-file .env \
        -e ESTADO_API_BASE_URL="http://estado-app:8080" \
        "$image" >/dev/null

    if docker run --rm --network estado_internal curlimages/curl:8.11.1 sh -c "
        for i in \$(seq 1 30); do
            curl -sf http://${NEXT}:8080/actuator/health >/dev/null 2>&1 && exit 0
            sleep 2
        done
        exit 1
    "; then
        return 0
    fi

    docker logs "$NEXT" --tail 50 2>&1 || true
    docker rm -f "$NEXT" >/dev/null 2>&1 || true
    return 1
}

# sha completo do commit que gerou a imagem, via label OCI padrao (setado
# automaticamente pelo docker/metadata-action no publish, com format=long
# pra bater exatamente com a tag por sha publicada no GHCR).
image_revision() {
    docker inspect "$1" --format '{{ index .Config.Labels "org.opencontainers.image.revision" }}' 2>/dev/null || true
}

promote() {
    docker rm -f "$CURRENT" >/dev/null 2>&1 || true
    docker rename "$NEXT" "$CURRENT"
}

# Marca no Grafana quando um deploy/rollback aconteceu, pra correlacionar
# visualmente com mudanca de latencia/erro no dashboard - mesmo padrao do
# repo estado (ver deploy/estado/lib-swap.sh la, ADR 0012). Melhor esforco
# de proposito: GRAFANA_CLOUD_URL/GRAFANA_CLOUD_ANNOTATIONS_TOKEN nao
# configurados, ou Grafana fora do ar, nunca falham o deploy - a anotacao
# e so uma conveniencia de observabilidade, nao faz parte do caminho
# critico do swap.
annotate_deploy() {
    local texto="$1"
    local tags="$2"

    if [ -z "${GRAFANA_CLOUD_URL:-}" ] || [ -z "${GRAFANA_CLOUD_ANNOTATIONS_TOKEN:-}" ]; then
        return 0
    fi

    curl -sf --max-time 5 -X POST "${GRAFANA_CLOUD_URL}/api/annotations" \
        -H "Authorization: Bearer ${GRAFANA_CLOUD_ANNOTATIONS_TOKEN}" \
        -H "Content-Type: application/json" \
        -d "{\"text\":\"${texto}\",\"tags\":${tags}}" >/dev/null 2>&1 || true
}
