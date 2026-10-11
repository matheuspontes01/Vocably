package com.vocably.dictionary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * O MockRestServiceServer intercepta as chamadas do RestClient: nenhum request sai para a
 * internet e nós decidimos o que a "API" responde. O JSON abaixo segue o formato real do
 * Free Dictionary API (resumido).
 */
class FreeDictionaryClientTest {

    static final String BASE_URL = "https://dictionary.test";

    MockRestServiceServer server;
    FreeDictionaryClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FreeDictionaryClient(builder, new DictionaryProperties(
                new DictionaryProperties.Endpoint(BASE_URL), new DictionaryProperties.Translation("unused", null)));
    }

    @Test
    void extractsPhoneticAudioAndOneDefinitionPerPartOfSpeech() {
        server.expect(requestTo(BASE_URL + "/api/v2/entries/en/bank"))
                .andRespond(withSuccess("""
                        [{
                          "word": "bank",
                          "phonetics": [
                            {"text": "/bæŋk/", "audio": "https://api.dictionaryapi.dev/media/pronunciations/en/bank-uk.mp3"},
                            {"text": "/bæŋk/", "audio": "https://api.dictionaryapi.dev/media/pronunciations/en/bank-us.mp3"}
                          ],
                          "meanings": [
                            {"partOfSpeech": "noun", "definitions": [
                              {"definition": "An institution where one can place and borrow money.", "example": "I went to the bank."},
                              {"definition": "The edge of a river."}
                            ], "synonyms": []},
                            {"partOfSpeech": "verb", "definitions": [
                              {"definition": "To deposit money in a bank."}
                            ]}
                          ],
                          "license": {"name": "CC BY-SA 3.0"}
                        }]
                        """, MediaType.APPLICATION_JSON));

        DictionaryEntry entry = client.lookup("bank").orElseThrow();

        assertThat(entry.phonetic()).isEqualTo("/bæŋk/");
        assertThat(entry.audioUrl()).endsWith("bank-us.mp3");
        assertThat(entry.meanings()).extracting(DictionaryEntry.Meaning::partOfSpeech).containsExactly("noun", "verb");
        assertThat(entry.meanings().getFirst().example()).isEqualTo("I went to the bank.");
    }

    @Test
    void returnsEmptyForUnknownWord() {
        server.expect(requestTo(BASE_URL + "/api/v2/entries/en/asdfgh"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"title\": \"No Definitions Found\"}"));

        assertThat(client.lookup("asdfgh")).isEmpty();
    }

    @Test
    void returnsEmptyWhenApiIsDown() {
        server.expect(requestTo(BASE_URL + "/api/v2/entries/en/bank")).andRespond(withServerError());

        assertThat(client.lookup("bank")).isEqualTo(Optional.empty());
    }
}
