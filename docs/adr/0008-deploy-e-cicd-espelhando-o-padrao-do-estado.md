# 8. Deploy e CI/CD Espelhando o Padrão do `estado`

Data: 2026-09-21

## Status

Aceito

## Contexto

Não havia processo de deploy nem scan de segurança automatizado. O repositório `estado` já validou, em produção, um padrão de deploy sem downtime e um pipeline de CI/CD com verificações de segurança - reaproveitar esse padrão testado é mais barato e mais seguro do que desenhar um novo do zero para este serviço, que roda no mesmo host/rede Docker.

## Decisão

- **Deploy**: rolling swap sem canário/blue-green (`deploy.sh`/`lib-swap.sh`), gated por health-check real em `/actuator/health` antes de promover o container novo; rollback manual via tag gravada automaticamente em `last-good-tag` a cada deploy bem-sucedido (`rollback.sh`); execução automática a cada 5 minutos via systemd timer, que faz *pull* da imagem `latest` publicada no GHCR.
- **CI/CD**: `ci.yml` roda PMD + testes em toda PR/push; `codeql.yml` faz scan estático semanal e em toda PR; `docker-publish.yml` só builda/publica a partir de um `push` confirmado em `main` (nunca a partir do head de um PR, mesmo de fork, para não permitir que um PR malicioso publique imagem própria usando o `GITHUB_TOKEN` do job); todas as actions de terceiros são fixadas por SHA de commit, não por tag mutável.
- **Dependências**: Dependabot agrupado semanalmente, com auto-merge condicionado a checks obrigatórios passarem e explicitamente excluindo bumps major (sempre revisados manualmente); `check-kotlin-stdlib-common.yml` monitora semanalmente se um pacote que parou de publicar jar real (por isso pinado manualmente e ignorado no Dependabot) voltou a publicar, abrindo uma issue automaticamente quando isso acontecer.
- **Rede**: o container fica só na rede Docker interna compartilhada com o `estado` (`estado_internal`), nunca exposto publicamente - evita que alguém contorne o rate limit por IP forjando `X-Forwarded-For` diretamente contra este serviço, já que `server.forward-headers-strategy=framework` confia no header sem validar a cadeia de proxy.

## Consequências

- Deploy e CI/CD deste serviço seguem exatamente o mesmo padrão operacional já validado no `estado`, reduzindo a superfície de decisões novas e de erro operacional.
- Um bug ou lacuna de segurança encontrado nesse padrão (no `estado` ou aqui) deveria, por consistência, ser corrigido nos dois repositórios.
- Métricas/tracing deste serviço ainda não são coletados pelo Grafana Alloy já existente no host (ver ADR 0007) - só os logs, via descoberta automática de containers Docker.
