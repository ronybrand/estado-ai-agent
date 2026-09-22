# 5. Handler Genérico de Exceção no Contrato de Erro

Data: 2026-09-21

## Status

Aceito

## Contexto

O `GlobalExceptionHandler` só mapeava `MethodArgumentNotValidException` (400) e `UpstreamServiceException` (502). Qualquer outra exceção não prevista (ex.: `NullPointerException`, uma falha interna do próprio Spring AI) não caía em nenhum handler dedicado e o Spring Boot respondia com seu formato de erro padrão, diferente do `ErrorResponse` (`code`, `message`, `requestId`) usado no resto da API - quebrando o contrato de erro exatamente no cenário mais imprevisível (um bug não antecipado).

## Decisão

Adicionar um `@ExceptionHandler(Exception.class)` como rede de segurança, respondendo sempre 500 com o código `ASK-99` e uma mensagem genérica ("Erro interno inesperado"). A mensagem original da exceção nunca vai para o corpo da resposta - só para o log - para não vazar detalhe interno (nome de classe, stacktrace, mensagem de driver/biblioteca) para o cliente.

## Consequências

- Todo erro possível em `/ask`, previsto ou não, mantém o mesmo contrato JSON, incluindo o `requestId` para correlação com o log.
- Um bug novo e não mapeado nunca mais expõe o formato de erro padrão do Spring Boot nem detalhe interno da exceção.
- Um novo tipo de falha que mereça um código de erro específico (em vez de cair no `ASK-99` genérico) ainda precisa de um handler dedicado dando append à lista - este handler genérico é o piso, não substitui handlers específicos.
