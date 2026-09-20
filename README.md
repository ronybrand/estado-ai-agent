# Estado AI Agent

Um agente de inteligência artificial (LLM) focado em consultar os dados reais e vivos da API pública do projeto `estado` (`https://54.94.231.248.sslip.io/estado/**`). 

O sistema utiliza o Google Gemini (Free Tier) via Spring AI 2.0+ com a funcionalidade de *Tool Calling*, provando que a IA pode consumir dados externos atualizados em vez de se limitar ao conhecimento do seu treinamento base.

## Funcionalidades

- **Endpoint `/ask`**: Envie uma pergunta sobre os estados brasileiros (ex: *"Qual a sigla de Santa Catarina?"* ou *"Quantos estados tem no banco?"*). O agente vai:
  1. Identificar a intenção e os parâmetros.
  2. Acionar uma `Tool` em Java para bater no endpoint real (`GET /estado/paginado` ou `GET /estado/{id}`).
  3. Receber o JSON da resposta HTTP, entender os dados, e compor a resposta final ao usuário em linguagem natural.
- **Proteção de Quota**: Como a API é pública e o Gemini tem rate limit severo no Free Tier (15 rpm), a aplicação implementa um Rate Limiting In-Memory (Bucket4j) de 10 requisições por minuto por IP.
- **Qualidade e Padrões**: PMD (Best Practices + Error Prone) e JaCoCo com CI/CD, mantendo o padrão adotado no projeto `spring-order-api`.

## Requisitos

- Java 25
- Maven (usar o `./mvnw` incluso)
- Uma API Key do Google Gemini (exportada como a variável de ambiente necessária pelo Spring AI)

## Como rodar localmente

1. Obtenha uma API key do Google Gemini (gratuita) em `ai.google.dev`.
2. Exporte a variável de ambiente: `export SPRING_AI_GOOGLE_GENAI_API_KEY="sua-chave-aqui"`
3. Execute o projeto: `./mvnw spring-boot:run`
4. Teste a API:
   ```bash
   curl -X POST http://localhost:8080/ask \
        -H "Content-Type: application/json" \
        -d '{"question":"Qual a sigla do estado do Paraná?"}'
   ```

## Testes

Os testes não geram custo de quota e nem dependem da rede ou da disponibilidade do Gemini / API do Estado. Usamos `@RestClientTest` e mocks explícitos do `ChatClient` do Spring AI para garantir um CI rápido e resiliente.

Execute: `./mvnw verify`

## ADRs

Para entender as decisões de arquitetura e design deste repositório, consulte a pasta `docs/adr/`.
