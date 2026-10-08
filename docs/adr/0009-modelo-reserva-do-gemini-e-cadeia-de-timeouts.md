# 9. Modelo reserva do Gemini e cadeia de timeouts do /ask

Data: 2026-10-08

## Status

Aceito

## Contexto

Em produção, `/ask` falhou em 4 de 6 perguntas de teste (502 depois de 36 a 54 s). A causa era o modelo
principal (`gemini-3.5-flash-lite`, free tier): chamado direto do servidor, uma vez levou 13,7 s para um
"ola" e na seguinte devolveu `503 UNAVAILABLE` ("high demand"). As duas respostas que funcionaram levaram
25,6 s e 33 s de ponta a ponta, acima do `read-timeout` de 25 s que o backend tinha acabado de ganhar.

## Decisão

**Modelo reserva.** Quando o principal falha por capacidade (HTTP 503), cota (HTTP 429) ou lentidão
(timeout), o `AskService` tenta uma vez o modelo de `app.gemini.fallback-model` (padrão
`gemini-3.1-flash-lite`; vazio desliga). Só essas falhas valem a segunda tentativa
(`GeminiFailures`): 400, 401 e 403 falhariam do mesmo jeito no outro modelo e mascarar o erro esconderia
o bug. Se o reserva também falha, a falha dele é propagada com a do principal como suprimida.

**Escolha do reserva.** Medida contra a chave de produção: `gemini-3.1-flash-lite` respondeu 200 em
0,75 s e aceitou tool calling, requisito do agente; os `gemini-2.5-*` devolveram 404.

**Cadeia de timeouts.** Cada camada precisa esperar mais do que a de baixo, para o erro tratável
chegar ao usuário antes de alguém desistir:

| Camada | Timeout |
|---|---|
| Angular | 60 s |
| Backend `estado` → agente (`ask-api.read-timeout-ms`) | 45 s |
| Agente → Gemini, por tentativa (`app.gemini.timeout-ms`) | 15 s, até duas tentativas (~30 s) |

## Alternativas consideradas

- **Outro provider como reserva.** Cobriria queda do Gemini inteiro, mas exige outro starter do Spring AI
  (memória do agente, que já usa ~170 de 288 MB), outra chave, prompt e guardas só validados no Gemini, e
  poucos providers têm API gratuita. O que se observou foi saturação de um modelo, não do provider.
- **Subir os timeouts sem reserva.** Mais de 45 a 60 s na tela parece travado e cada pergunta lenta
  segura uma thread do servlet do backend (que também serve o CRUD).
- **Repetir no mesmo modelo.** Não ajuda quando o modelo está saturado.

## Consequências

- Positivo: uma queda de capacidade de um modelo deixa de derrubar o `/ask`.
- Negativo aceito: no pior caso uma pergunta consome duas chamadas de cota, e o reserva pode responder com
  qualidade ou estilo diferentes. O log registra a troca (`tentando o modelo reserva ...`); não há métrica
  própria ainda.
- Os modelos disponíveis para a chave mudam (os 2.5 sumiram); `ListModels` antes de trocar o padrão.
- Os limites do free tier são por modelo e mudam; conferir no painel antes de contar com uma cota.
