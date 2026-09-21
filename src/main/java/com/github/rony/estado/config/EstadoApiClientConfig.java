package com.github.rony.estado.config;

import com.github.rony.estado.observability.RequestIdPropagationInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class EstadoApiClientConfig {

    @Bean
    public RestClient estadoRestClient(
            @Value("${estado.api.base-url}") String baseUrl,
            @Value("${estado.api.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${estado.api.read-timeout-ms:5000}") long readTimeoutMs) {

        HttpClientSettings settings = HttpClientSettings.defaults()
                .withTimeouts(Duration.ofMillis(connectTimeoutMs), Duration.ofMillis(readTimeoutMs));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(new RequestIdPropagationInterceptor())
                .build();
    }
}
