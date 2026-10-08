package com.github.rony.estado.ask;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.List;

import com.google.genai.errors.ApiException;

// Quais falhas do Gemini justificam tentar o modelo reserva: so as de
// capacidade (503 "high demand", 429 de cota) e as de lentidao (timeout).
// Requisicao invalida (400) ou chave sem permissao (401/403) falhariam do
// mesmo jeito no outro modelo, entao sao propagadas sem mascarar o bug.
final class GeminiFailures {

    private static final int SERVICO_INDISPONIVEL = 503;
    private static final int COTA_EXCEDIDA = 429;
    private static final List<Integer> CODIGOS_DE_CAPACIDADE = List.of(SERVICO_INDISPONIVEL, COTA_EXCEDIDA);

    private GeminiFailures() {
    }

    static boolean valeTentarOutroModelo(Throwable falha) {
        for (Throwable causa = falha; causa != null; causa = causa.getCause()) {
            if (causa instanceof ApiException api) {
                return CODIGOS_DE_CAPACIDADE.contains(api.code());
            }
            if (causa instanceof InterruptedIOException || causa instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
