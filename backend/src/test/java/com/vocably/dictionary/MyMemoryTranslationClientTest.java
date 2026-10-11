package com.vocably.dictionary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Os JSONs seguem o formato real do MyMemory (resumido). */
class MyMemoryTranslationClientTest {

    static final String BASE_URL = "https://translate.test";

    MockRestServiceServer server;
    MyMemoryTranslationClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new MyMemoryTranslationClient(builder, new DictionaryProperties(
                new DictionaryProperties.Endpoint("unused"), new DictionaryProperties.Translation(BASE_URL, "me@example.com")));
    }

    @Test
    void returnsTranslationAndCleanAlternatives() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/get")))
                .andExpect(queryParam("q", "bank"))
                .andExpect(queryParam("langpair", "en%7Cpt-BR"))
                .andExpect(queryParam("de", "me@example.com"))
                .andRespond(withSuccess("""
                        {
                          "responseData": {"translatedText": "banco", "match": 1},
                          "quotaFinished": false,
                          "responseStatus": 200,
                          "matches": [
                            {"segment": "bank", "translation": "banco", "quality": "74"},
                            {"segment": "bank", "translation": "margem.", "quality": "70"},
                            {"segment": "Bank", "translation": "Banco", "quality": "70"},
                            {"segment": "river bank", "translation": "beira do rio", "quality": "60"},
                            {"segment": "bank", "translation": "barranco", "quality": "50"}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        Translation translation = client.translate("bank").orElseThrow();

        assertThat(translation.text()).isEqualTo("banco");
        // Sem repetir "banco", sem o ponto final e sem o trecho "river bank".
        assertThat(translation.alternatives()).containsExactly("margem", "barranco");
    }

    @Test
    void returnsEmptyWhenQuotaIsExceeded() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/get")))
                .andRespond(withSuccess("""
                        {"responseData": {"translatedText": "MYMEMORY WARNING: YOU USED ALL AVAILABLE FREE TRANSLATIONS FOR TODAY"},
                         "quotaFinished": true, "responseStatus": 429, "matches": []}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.translate("bank")).isEmpty();
    }
}
