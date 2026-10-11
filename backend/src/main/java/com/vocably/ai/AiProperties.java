package com.vocably.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração da integração com a API do Claude (chaves vocably.ai.* do application.yml).
 *
 * @param apiKey chave da API da Anthropic; vazia = recursos de IA desligados
 * @param model  modelo usado nas chamadas
 */
@ConfigurationProperties(prefix = "vocably.ai")
public record AiProperties(String apiKey, String model) {

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
