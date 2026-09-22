# 7. Spring Boot Actuator para Observabilidade

Data: 2026-09-21

## Status

Aceito

## Contexto

Não havia nenhuma forma de checar a saúde da aplicação ou ver métricas (latência/contagem por endpoint e status HTTP, uso de memória, etc.) sem entrar diretamente no log. O `estado-ai-agent` roda no mesmo host/rede Docker do backend `estado`, que já centraliza logs de todos os containers via Grafana Alloy (`discovery.docker`, ver ADR 0012 do repositório `estado`) - então os logs já chegam ao Grafana Cloud automaticamente, mas não havia nenhuma métrica de aplicação exposta para ser coletada.

## Decisão

Adicionar `spring-boot-starter-actuator`, que traz Micrometer e instrumenta automaticamente `http.server.requests` (contagem/latência por endpoint e status). Expor deliberadamente só `health`, `info` e `metrics` via `management.endpoints.web.exposure.include` - nunca `env`, `beans` ou `configprops`, que vazariam configuração interna (ex.: valor de `GEMINI_API_KEY` resolvido). `management.endpoint.health.show-details=never` evita vazar detalhe de cada health indicator (ex.: stacktrace de falha de disco), já que `/actuator/**` fica fora do escopo do `ApiKeyAuthFilter` (que só protege `/ask`) - não há autenticação dedicada para esses endpoints.

## Consequências

- `/actuator/health` e `/actuator/metrics` ficam disponíveis para health-check do deploy (rolling swap, ver `deploy/lib-swap.sh`) e para inspeção manual, sem exigir API key.
- Métricas ainda não são coletadas por um agente (Prometheus/Alloy) neste serviço - diferente do backend `estado`, que já tem essa extensão planejada via `/actuator/prometheus` (ADR 0012 do repositório `estado`). Adicionar `micrometer-registry-prometheus` e apontar o Alloy existente para este container é a extensão natural, ainda não feita.
- Tracing distribuído completo (spans/timings entre `estado-ai-agent` e a API `estado`) continua fora de escopo - exigiria um exportador (`micrometer-tracing` + OTLP/Zipkin) nos dois serviços. A correlação entre eles hoje é feita só via `X-Request-Id` compartilhado nos logs (ver commit que adicionou `RequestIdPropagationInterceptor`), que cobre boa parte do valor prático sem essa infraestrutura extra.
