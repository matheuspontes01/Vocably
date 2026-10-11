package com.vocably.dictionary;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

@Configuration
@EnableCaching
@EnableConfigurationProperties(DictionaryProperties.class)
public class DictionaryConfig {

    /**
     * Timeouts para as chamadas externas: se a API gratuita estiver lenta, desistimos em
     * poucos segundos em vez de deixar o usuário esperando indefinidamente.
     */
    @Bean
    RestClientCustomizer timeoutsCustomizer() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return builder -> builder.requestFactory(factory);
    }
}
