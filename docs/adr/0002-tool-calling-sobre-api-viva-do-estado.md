# 2. Tool Calling Sobre API Viva do `estado`

Data: 2026-09-19

## Status

Aceito

## Contexto

Vagas de IA no mercado frequentemente pedem experiência com `tool calling` (também conhecido como function calling). Precisamos demonstrar essa habilidade sem criar um domínio de mentira que gaste tempo desnecessário.

## Decisão

O agente vai expor um endpoint que, via *tool calling*, consulta dinamicamente a API pública real já existente do projeto `estado` (`https://54.94.231.248.sslip.io/estado/**`).

## Consequências

- Integração com um sistema de portfólio real e rodando em produção.
- Sem necessidade de mockar dados, subir banco de dados ou duplicar esforço com domínio genérico.
- O agente depende da disponibilidade da API do `estado`.
