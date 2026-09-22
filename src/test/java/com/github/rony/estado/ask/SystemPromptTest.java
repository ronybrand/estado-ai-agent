package com.github.rony.estado.ask;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemPromptTest {

    @Test
    void shouldInstructModelToAnswerInTheSameLanguageAsTheQuestion() {
        assertThat(SystemPrompt.INTERNAL_RULES)
                .contains("mesmo idioma em que a pergunta foi feita");
    }
}
