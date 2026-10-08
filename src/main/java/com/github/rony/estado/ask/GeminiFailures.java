package com.github.rony.estado.ask;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;

import com.google.genai.errors.ApiException;

// Quais falhas do Gemini justificam tentar o modelo reserva: so as de
// capacidade (503 "high demand", 429 de cota) e as de lentidao (timeout).
// Requisicao invalida (400) ou chave sem permissao (401/403) falhariam do
// mesmo jeito no outro modelo, entao sao propagadas sem mascarar o bug.
final class GeminiFailures {

    private static final int SERVICO_INDISPONIVEL = 503;
    private static final int COTA_EXCEDIDA = 429;

    private GeminiFailures() {
    }

    static boolean valeTentarOutroModelo(Throwable falha) {
        for (Throwable causa = falha; causa != null; causa = causa.getCause()) {
            if (causa instanceof ApiException api) {
                return api.code() == SERVICO_INDISPONIVEL || api.code() == COTA_EXCEDIDA;
            }
            if (causa instanceof InterruptedIOException || causa instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
