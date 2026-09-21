package com.github.rony.estado.ask;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AskControllerTest {

    @Mock
    private AskService askService;

    @InjectMocks
    private AskController askController;

    @Test
    void shouldDelegateToAskServiceAndReturnItsResponse() {
        AskRequest request = new AskRequest("Qual a capital do Brasil?");
        when(askService.ask(request)).thenReturn(new AskResponse("Mocked Answer"));

        AskResponse response = askController.ask(request);

        assertEquals("Mocked Answer", response.answer());
    }
}
