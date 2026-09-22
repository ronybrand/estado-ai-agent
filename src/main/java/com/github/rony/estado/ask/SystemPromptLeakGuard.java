package com.github.rony.estado.ask;

import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

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
//
// Antes de comparar, tanto as linhas protegidas quanto a resposta sao
// normalizadas (acentos, pontuacao e espacos removidos) para que variacoes
// triviais de formatacao do modelo (espacos extras, quebras de linha,
// pontuacao diferente, acentuacao alterada) nao escapem da deteccao. Isso
// nao protege contra parafrase real, traducao ou encoding (ex.: base64) -
// e uma limitacao conhecida do match literal, fora do escopo desta guarda.
public final class SystemPromptLeakGuard {

    private static final int MIN_LINE_LENGTH = 30;

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");

    private static final List<String> PROTECTED_LINES = Arrays.stream(SystemPrompt.INTERNAL_RULES.split("\n"))
            .map(String::trim)
            .filter(line -> line.length() >= MIN_LINE_LENGTH)
            .map(SystemPromptLeakGuard::normalize)
            .toList();

    private SystemPromptLeakGuard() {
    }

    public static boolean isLeaking(String answer) {
        if (StringUtils.isBlank(answer)) {
            return false;
        }
        String normalizedAnswer = normalize(answer);
        return PROTECTED_LINES.stream().anyMatch(normalizedAnswer::contains);
    }

    private static String normalize(String text) {
        String withoutAccents = DIACRITICS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD))
                .replaceAll("");
        String lettersAndDigitsOnly = NON_ALPHANUMERIC.matcher(withoutAccents.toLowerCase())
                .replaceAll(" ");
        return MULTIPLE_SPACES.matcher(lettersAndDigitsOnly).replaceAll(" ").trim();
    }
}
