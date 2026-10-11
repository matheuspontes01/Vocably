package com.vocably.dictionary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Cliente do MyMemory (https://mymemory.translated.net): tradução gratuita, sem chave.
 * Exemplo: GET /get?q=bank&langpair=en|pt-BR
 */
@Component
public class MyMemoryTranslationClient {

    private static final Logger log = LoggerFactory.getLogger(MyMemoryTranslationClient.class);
    static final int MAX_ALTERNATIVES = 3;

    private final RestClient http;
    private final String email;

    public MyMemoryTranslationClient(RestClient.Builder builder, DictionaryProperties properties) {
        this.http = builder.baseUrl(properties.translation().baseUrl()).build();
        this.email = properties.translation().email();
    }

    /** Traduz do inglês para o português do Brasil. Vazio se a API falhar ou a cota do dia acabar. */
    @Cacheable("translations")
    public Optional<Translation> translate(String text) {
        try {
            ApiResponse response = http.get()
                    .uri(uri -> {
                        uri.path("/get").queryParam("q", text).queryParam("langpair", "en|pt-BR");
                        if (email != null && !email.isBlank()) {
                            uri.queryParam("de", email);
                        }
                        return uri.build();
                    })
                    .retrieve()
                    .body(ApiResponse.class);
            return toTranslation(text, response);
        } catch (RestClientException e) {
            log.warn("MyMemory translation failed for '{}': {}", text, e.getMessage());
            return Optional.empty();
        }
    }

    static Optional<Translation> toTranslation(String query, ApiResponse response) {
        // Em caso de erro (ex.: cota esgotada) a API responde 200 com responseStatus diferente de 200.
        if (response == null || !"200".equals(response.responseStatus()) || response.responseData() == null) {
            return Optional.empty();
        }
        String main = clean(response.responseData().translatedText());
        if (main.isEmpty()) {
            return Optional.empty();
        }

        // Outras traduções vêm da "memória de tradução": tiramos repetidas e a própria principal.
        Set<String> seen = new LinkedHashSet<>();
        seen.add(main.toLowerCase(Locale.ROOT));
        List<String> alternatives = Objects.requireNonNullElse(response.matches(), List.<ApiMatch>of()).stream()
                // Só traduções do mesmo texto: a memória também traz trechos "parecidos", que confundem.
                .filter(m -> clean(m.segment()).equalsIgnoreCase(clean(query)))
                .map(m -> clean(m.translation()))
                .filter(t -> !t.isEmpty() && t.length() <= 60)
                .filter(t -> seen.add(t.toLowerCase(Locale.ROOT)))
                .limit(MAX_ALTERNATIVES)
                .toList();
        return Optional.of(new Translation(main, alternatives));
    }

    /** Remove espaços e pontuação final que a memória de tradução às vezes inclui ("banco." → "banco"). */
    private static String clean(String text) {
        return text == null ? "" : text.trim().replaceAll("[.!?;:]+$", "").trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ApiResponse(ApiData responseData, String responseStatus, List<ApiMatch> matches) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ApiData(String translatedText) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ApiMatch(String segment, String translation) {
    }
}
