package com.github.rony.estado;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.ai.google.genai.api-key=dummy-test-key",
		"estado.api.base-url=http://localhost:0",
		"app.security.api-key=dummy-api-key",
		"app.security.cors-allowed-origins=http://localhost:4200"
})
class EstadoAiAgentApplicationTests {

	@Test
	void contextLoads() {
	}

}
