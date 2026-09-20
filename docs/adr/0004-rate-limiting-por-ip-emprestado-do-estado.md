# 4. Rate Limiting por IP (Emprestado do Estado)

Data: 2026-09-19

## Status

Aceito

## Contexto

Como estamos usando o Free Tier do Gemini e expondo um endpoint público (`/ask`) desprotegido, qualquer script malicioso pode inundar a API e estourar nossos limites da API Key.

## Decisão

Implementaremos Rate Limiting por IP utilizando a biblioteca Bucket4j como um Filtro simples (Servlet Filter), seguindo o mesmo padrão validado pelo ADR 0016 do repositório `estado`.

## Consequências

- Proteção imediata contra abusos que estourariam a quota do Free Tier.
- Mantém a API aberta para demonstrações pontuais por recrutadores, mas recusa surtos.
- A configuração reside em memória no `estado-ai-agent`. Se ele for escalado horizontalmente algum dia, exigirá um store distribuído, mas para o portfólio, *in-memory* atende perfeitamente.
