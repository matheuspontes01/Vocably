package com.vocably.word;

import com.vocably.TestcontainersConfiguration;
import com.vocably.auth.User;
import com.vocably.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
    WordRepository words;

    @Autowired
    UserRepository users;

    User alice;
    User bob;

    @BeforeEach
    void setUp() {
        words.deleteAll();
        users.deleteAll();
        alice = users.save(new User("alice@example.com", "hash", "Alice"));
        bob = users.save(new User("bob@example.com", "hash", "Bob"));
    }

    /** Simula uma requisição autenticada como esse usuário, sem precisar gerar um token de verdade. */
    static RequestPostProcessor as(User user) {
        return jwt().jwt(token -> token.subject(user.getId().toString()));
    }

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mvc.perform(get("/api/words"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsAndFetchesWord() throws Exception {
        String location = mvc.perform(post("/api/words").with(as(alice))
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

        mvc.perform(get(location).with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.term").value("ephemeral"))
                .andExpect(jsonPath("$.sourceType").value("BOOK"));
    }

    @Test
    void rejectsBlankTerm() throws Exception {
        mvc.perform(post("/api/words").with(as(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"term": "  "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns404ForUnknownWord() throws Exception {
        mvc.perform(get("/api/words/999999").with(as(alice)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Word 999999 not found"));
    }

    @Test
    void searchesByTermIgnoringCase() throws Exception {
        words.save(new Word(alice.getId(), "Resilient", null, null, SourceType.OTHER, null));
        words.save(new Word(alice.getId(), "brittle", null, null, SourceType.OTHER, null));

        mvc.perform(get("/api/words").param("search", "resil").with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].term").value("Resilient"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void updatesAndDeletesWord() throws Exception {
        Word word = words.save(new Word(alice.getId(), "cumbersome", null, null, SourceType.OTHER, null));

        mvc.perform(put("/api/words/{id}", word.getId()).with(as(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"term": "cumbersome", "translation": "trabalhoso", "sourceType": "VIDEO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translation").value("trabalhoso"))
                .andExpect(jsonPath("$.sourceType").value("VIDEO"));

        mvc.perform(delete("/api/words/{id}", word.getId()).with(as(alice)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/words/{id}", word.getId()).with(as(alice)))
                .andExpect(status().isNotFound());
    }

    @Test
    void usersCannotSeeOrChangeEachOthersWords() throws Exception {
        Word aliceWord = words.save(new Word(alice.getId(), "secret", null, null, SourceType.OTHER, null));

        mvc.perform(get("/api/words").with(as(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        mvc.perform(get("/api/words/{id}", aliceWord.getId()).with(as(bob)))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/words/{id}", aliceWord.getId()).with(as(bob)))
                .andExpect(status().isNotFound());
    }
}
