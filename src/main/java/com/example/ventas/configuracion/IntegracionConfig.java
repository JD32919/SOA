
package com.example.ventas.configuracion;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class IntegracionConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient catalogoRestClient(
            RestClient.Builder builder,
            @Value("${servicios.catalogo.url}") String url,
            @Value("${servicios.timeout-ms:5000}") int timeout) {
        return crear(builder, url, timeout);
    }

    @Bean
    RestClient clientesRestClient(
            RestClient.Builder builder,
            @Value("${servicios.clientes.url}") String url,
            @Value("${servicios.timeout-ms:5000}") int timeout) {
        return crear(builder, url, timeout);
    }

    private RestClient crear(RestClient.Builder builder, String url, int timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeout));
        factory.setReadTimeout(Duration.ofMillis(timeout));

        return builder.clone()
                .baseUrl(url)
                .requestFactory(factory)
                .build();
    }
}
