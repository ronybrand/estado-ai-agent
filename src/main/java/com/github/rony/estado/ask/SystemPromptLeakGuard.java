package com.github.rony.estado.ask;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

// Guarda de saida deterministica contra vazamento do system prompt: a defesa
// no proprio prompt (instrucao para o modelo nunca revelar suas regras) e
// probabilistica - um prompt injection bem construido ainda pode fazer o
// modelo obedecer e repetir as instrucoes internas na resposta. Isso nao
// depende do modelo "decidir" seguir a regra: verifica, por match literal,
// se a resposta reproduz uma linha inteira e especifica do system prompt.
//
// So considera linhas com um tamanho minimo (MIN_LINE_LENGTH) para evitar
// falso positivo em frases curtas ou genericas que podem aparecer por
// coincidencia numa resposta legitima (ex.: "estados brasileiros").
public final class SystemPromptLeakGuard {

    private static final int MIN_LINE_LENGTH = 30;

    private SystemPromptLeakGuard() {
    }

    public static boolean isLeaking(String answer, String systemPrompt) {
        if (StringUtils.isBlank(answer)) {
            return false;
        }
        String normalizedAnswer = answer.toLowerCase();
        return Arrays.stream(systemPrompt.split("\n"))
                .map(String::trim)
                .filter(line -> line.length() >= MIN_LINE_LENGTH)
                .map(String::toLowerCase)
                .anyMatch(normalizedAnswer::contains);
    }
}
