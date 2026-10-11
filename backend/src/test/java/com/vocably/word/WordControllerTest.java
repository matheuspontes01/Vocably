package com.vocably.word;

import com.vocably.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração: sobe a aplicação inteira com um PostgreSQL real (Testcontainers)
 * e faz requisições HTTP simuladas com o MockMvc — do controller até o banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WordControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    WordRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void createsAndFetchesWord() throws Exception {
        String location = mvc.perform(post("/api/words")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"term": "ephemeral", "translation": "efêmero",
                                 "contextSentence": "Fame is ephemeral.", "sourceType": "BOOK", "sourceTitle": "Dune"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", notNullValue()))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.term").value("ephemeral"))
                .andExpect(jsonPath("$.sourceType").value("BOOK"));
    }

    @Test
    void rejectsBlankTerm() throws Exception {
        mvc.perform(post("/api/words")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"term": "  "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns404ForUnknownWord() throws Exception {
        mvc.perform(get("/api/words/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Word 999999 not found"));
    }

    @Test
    void searchesByTermIgnoringCase() throws Exception {
        repository.save(new Word("Resilient", null, null, SourceType.OTHER, null));
        repository.save(new Word("brittle", null, null, SourceType.OTHER, null));

        mvc.perform(get("/api/words").param("search", "resil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].term").value("Resilient"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void updatesAndDeletesWord() throws Exception {
        Word word = repository.save(new Word("cumbersome", null, null, SourceType.OTHER, null));

        mvc.perform(put("/api/words/{id}", word.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"term": "cumbersome", "translation": "trabalhoso", "sourceType": "VIDEO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translation").value("trabalhoso"))
                .andExpect(jsonPath("$.sourceType").value("VIDEO"));

        mvc.perform(delete("/api/words/{id}", word.getId()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/words/{id}", word.getId()))
                .andExpect(status().isNotFound());
    }
}
