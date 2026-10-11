package com.vocably.dictionary;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vocably")
public record DictionaryProperties(Endpoint dictionary, Translation translation) {

    public record Endpoint(String baseUrl) {
    }

    /** @param email opcional: o MyMemory aumenta o limite diário quando você informa um email */
    public record Translation(String baseUrl, String email) {
    }
}
