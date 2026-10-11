package com.vocably.dictionary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Cliente do Free Dictionary API (https://dictionaryapi.dev): gratuito e sem chave.
 * Exemplo: GET /api/v2/entries/en/bank
 */
@Component
public class FreeDictionaryClient {

    private static final Logger log = LoggerFactory.getLogger(FreeDictionaryClient.class);
    static final int MAX_MEANINGS = 4;

    private final RestClient http;

    public FreeDictionaryClient(RestClient.Builder builder, DictionaryProperties properties) {
        this.http = builder.baseUrl(properties.dictionary().baseUrl()).build();
    }

    /**
     * @return vazio se a palavra não existir no dicionário ou se a API estiver fora do ar.
     * O resultado fica em cache: pesquisar "bank" de novo não chama a API outra vez.
     */
    @Cacheable("dictionary")
    public Optional<DictionaryEntry> lookup(String word) {
        try {
            List<ApiEntry> entries = http.get()
                    .uri("/api/v2/entries/en/{word}", word)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return entries == null || entries.isEmpty() ? Optional.empty() : Optional.of(toEntry(entries));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty(); // a API responde 404 quando não conhece a palavra
        } catch (RestClientException e) {
            log.warn("Free Dictionary API failed for '{}': {}", word, e.getMessage());
            return Optional.empty();
        }
    }

    /** A API devolve várias "entradas"; juntamos tudo e pegamos o primeiro valor útil de cada coisa. */
    private static DictionaryEntry toEntry(List<ApiEntry> entries) {
        String phonetic = null;
        String audioUrl = null;
        List<DictionaryEntry.Meaning> meanings = new ArrayList<>();

        for (ApiEntry entry : entries) {
            if (phonetic == null && notBlank(entry.phonetic())) {
                phonetic = entry.phonetic();
            }
            for (ApiPhonetic p : Objects.requireNonNullElse(entry.phonetics(), List.<ApiPhonetic>of())) {
                if (phonetic == null && notBlank(p.text())) {
                    phonetic = p.text();
                }
                // Prefere o áudio americano ("-us.mp3") quando existir.
                if (notBlank(p.audio()) && (audioUrl == null || p.audio().endsWith("-us.mp3"))) {
                    audioUrl = p.audio();
                }
            }
            for (ApiMeaning m : Objects.requireNonNullElse(entry.meanings(), List.<ApiMeaning>of())) {
                // Uma definição por classe gramatical (substantivo, verbo...) já dá uma boa visão.
                Objects.requireNonNullElse(m.definitions(), List.<ApiDefinition>of()).stream()
                        .filter(d -> notBlank(d.definition()))
                        .findFirst()
                        .ifPresent(d -> meanings.add(new DictionaryEntry.Meaning(m.partOfSpeech(), d.definition(), d.example())));
            }
        }
        return new DictionaryEntry(phonetic, audioUrl, meanings.stream().limit(MAX_MEANINGS).toList());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    // Records que espelham só a parte do JSON da API que nos interessa.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ApiEntry(String word, String phonetic, List<ApiPhonetic> phonetics, List<ApiMeaning> meanings) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ApiPhonetic(String text, String audio) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ApiMeaning(String partOfSpeech, List<ApiDefinition> definitions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ApiDefinition(String definition, String example) {
    }
}
