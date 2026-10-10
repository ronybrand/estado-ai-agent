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

# Mesma motivacao do repo estado (ver deploy/estado/lib-swap.sh la): a JVM padrao
# (G1, heap = 1/4 da RAM) ocupava ~225 MB aqui, e a instancia e t3.micro (1 GB).
# Sobrescreve-se via .env.
AGENT_JAVA_OPTS="${AGENT_JAVA_OPTS:--XX:+UseSerialGC -Xmx128m -Xss512k -XX:TieredStopAtLevel=1 -XX:MaxMetaspaceSize=112m -XX:ReservedCodeCacheSize=32m -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20}"
AGENT_MEMORY_LIMIT="${AGENT_MEMORY_LIMIT:-288m}"

swap_to() {
    local image="$1"

    docker rm -f "$NEXT" >/dev/null 2>&1 || true

    docker run -d --name "$NEXT" \
        --restart unless-stopped \
        --network estado_internal \
        --env-file .env \
        --memory "$AGENT_MEMORY_LIMIT" \
        -e JAVA_TOOL_OPTIONS="$AGENT_JAVA_OPTS" \
        -e ESTADO_API_BASE_URL="http://estado-app:8080" \
        "$image" >/dev/null

    if docker run --rm --network estado_internal curlimages/curl:8.11.1 sh -c "
        for i in \$(seq 1 90); do
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

# O container antigo continua no ar (renomeado) ate drenar_antigo: quem ainda
# tem o IP dele em cache (DNS da JVM, 30 s) ou uma conexao keep-alive aberta
# com ele seria atendido por um container ja removido (connect timeout / EOF).
# restart=no pro antigo nao voltar sozinho se o host reiniciar durante a espera.
# A imagem que esta saindo ganha a tag local "anterior": o prune de drenar_antigo
# nao a remove (deixa de ser dangling) e ./rollback.sh anterior funciona sem
# depender do registry. Cada deploy move a tag, entao so uma versao extra fica.
marcar_anterior() {
    local id
    id="$(docker inspect --format '{{.Image}}' "$CURRENT" 2>/dev/null || true)"
    if [ -n "$id" ] && [ -n "${IMAGE:-}" ]; then
        docker tag "$id" "${IMAGE%%:*}:anterior" >/dev/null 2>&1 || true
    fi
}

promote() {
    local antigo="${CURRENT}-antigo"
    marcar_anterior
    docker rm -f "$antigo" >/dev/null 2>&1 || true
    if docker rename "$CURRENT" "$antigo" >/dev/null 2>&1; then
        docker update --restart=no "$antigo" >/dev/null 2>&1 || true
    fi
    docker rename "$NEXT" "$CURRENT"
}

# Sincrona de proposito: o systemd mata processos em segundo plano quando o
# oneshot termina. Chamar por ultimo no deploy/rollback.
drenar_antigo() {
    local antigo="${CURRENT}-antigo"
    docker container inspect "$antigo" >/dev/null 2>&1 || return 0
    echo "Drenando $antigo por ${DRAIN_SECONDS:-60}s antes de remove-lo..."
    sleep "${DRAIN_SECONDS:-60}"
    docker stop -t 30 "$antigo" >/dev/null 2>&1 || true
    docker rm -f "$antigo" >/dev/null 2>&1 || true
    # Cada deploy deixa uma imagem sem tag; limpar aqui mantem o disco perto do
    # piso (a "anterior" fica). O timer semanal de prune segue como rede de seguranca.
    docker image prune -f >/dev/null 2>&1 || true
}

# Registra o resultado do swap na aba Deployments do GitHub - mesmo padrao do
# repo estado (ver notify_github_deployment em deploy/estado/lib-swap.sh la).
# O workflow so publica a imagem; quem sabe se o deploy deu certo e esta
# instancia (pull via timer), entao ela mesma avisa. Melhor esforco: sem
# GITHUB_DEPLOY_TOKEN, ou com a API fora do ar, o deploy nao falha.
# Uso: notify_github_deployment <sha-completo> <success|failure> <descricao>
notify_github_deployment() {
    local sha="$1"
    local state="$2"
    local descricao="$3"
    local api="https://api.github.com/repos/ronybrand/estado-ai-agent/deployments"

    if [ -z "${GITHUB_DEPLOY_TOKEN:-}" ] || [ -z "$sha" ]; then
        return 0
    fi

    local resposta id
    resposta="$(curl -sf --max-time 10 -X POST "$api" \
        -H "Authorization: Bearer ${GITHUB_DEPLOY_TOKEN}" \
        -H "Accept: application/vnd.github+json" \
        -d "{\"ref\":\"${sha}\",\"environment\":\"production\",\"auto_merge\":false,\"required_contexts\":[],\"description\":\"${descricao}\"}" \
        2>/dev/null)" || return 0
    id="$(echo "$resposta" | sed -n 's/^  "id": *\([0-9][0-9]*\),.*/\1/p' | head -1)"
    [ -n "$id" ] || return 0

    curl -sf --max-time 10 -X POST "${api}/${id}/statuses" \
        -H "Authorization: Bearer ${GITHUB_DEPLOY_TOKEN}" \
        -H "Accept: application/vnd.github+json" \
        -d "{\"state\":\"${state}\",\"environment_url\":\"https://api.ronybrand.click/ask\",\"description\":\"${descricao}\"}" \
        >/dev/null 2>&1 || true
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
