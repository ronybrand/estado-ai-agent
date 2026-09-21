package com.github.rony.estado.ask;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskRequest(

        @NotBlank(message = "question e obrigatoria")
        @Size(max = 1000, message = "question excede o tamanho maximo de 1000 caracteres")
        String question) {
}
