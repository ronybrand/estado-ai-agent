# 3. Testes Mockam ChatClient e HTTP, Não Batem em Serviço Externo

Data: 2026-09-19

## Status

Aceito

## Contexto

O processo de CI e a verificação local rodam com frequência. Se cada execução de teste de integração chamasse o Google Gemini e a API viva do `estado`, gastaríamos o Free Tier rapidamente e os testes seriam instáveis por depender de rede externa (flaky tests).

## Decisão

Os testes unitários e de integração mockarão as chamadas reais:
1. `ChatClient` será mockado no teste do Controller usando features de teste do Spring AI (ex: `@WebMvcTest`).
2. O `RestClient` será mockado no teste das Tools.

## Consequências

- CI rápido e resiliente.
- Zero custo de quota nas validações automatizadas.
- O fluxo "Fim a Fim" completo deve ser verificado localmente ou por outros meios de teste manual.
