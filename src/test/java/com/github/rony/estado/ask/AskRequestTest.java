package com.github.rony.estado.ask;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AskRequestTest {

    private static final int MAX_QUESTION_LENGTH = 1000;

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void shouldAcceptValidQuestion() {
        Set<ConstraintViolation<AskRequest>> violations = validator.validate(new AskRequest("Qual a capital do Brasil?"));

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectNullQuestion() {
        Set<ConstraintViolation<AskRequest>> violations = validator.validate(new AskRequest(null));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldRejectBlankQuestion() {
        Set<ConstraintViolation<AskRequest>> violations = validator.validate(new AskRequest("   "));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldRejectQuestionLongerThanMax() {
        String tooLong = "a".repeat(MAX_QUESTION_LENGTH + 1);

        Set<ConstraintViolation<AskRequest>> violations = validator.validate(new AskRequest(tooLong));

        assertThat(violations).isNotEmpty();
    }

    @Test
    void shouldAcceptQuestionAtMaxLength() {
        String atMax = "a".repeat(MAX_QUESTION_LENGTH);

        Set<ConstraintViolation<AskRequest>> violations = validator.validate(new AskRequest(atMax));

        assertThat(violations).isEmpty();
    }
}
