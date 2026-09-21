package com.github.rony.estado.ask;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

// Guarda de saida deterministica contra vazamento do system prompt: a defesa
// no proprio prompt (instrucao para o modelo nunca revelar suas regras) e
// probabilistica - um prompt injection bem construido ainda pode fazer o
// modelo obedecer e repetir as instrucoes internas na resposta. Isso nao
// depende do modelo "decidir" seguir a regra: verifica, por match literal,
// se a resposta reproduz uma linha inteira e especifica das regras internas.
//
// So verifica SystemPrompt.INTERNAL_RULES (nao SystemPrompt.DESCRIPTION):
// a descricao publica do assistente pode legitimamente aparecer parafraseada
// numa resposta normal, entao nao deve contar como vazamento.
//
// So considera linhas com um tamanho minimo (MIN_LINE_LENGTH) para evitar
// falso positivo em frases curtas ou genericas que podem aparecer por
// coincidencia numa resposta legitima (ex.: "estados brasileiros").
public final class SystemPromptLeakGuard {

    private static final int MIN_LINE_LENGTH = 30;

    private static final List<String> PROTECTED_LINES = Arrays.stream(SystemPrompt.INTERNAL_RULES.split("\n"))
            .map(String::trim)
            .filter(line -> line.length() >= MIN_LINE_LENGTH)
            .map(String::toLowerCase)
            .toList();

    private SystemPromptLeakGuard() {
    }

    public static boolean isLeaking(String answer) {
        if (StringUtils.isBlank(answer)) {
            return false;
        }
        String normalizedAnswer = answer.toLowerCase();
        return PROTECTED_LINES.stream().anyMatch(normalizedAnswer::contains);
    }
}
