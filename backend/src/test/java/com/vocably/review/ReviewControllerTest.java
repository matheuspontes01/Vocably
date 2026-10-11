package com.vocably.review;

import com.jayway.jsonpath.JsonPath;
import com.vocably.TestcontainersConfiguration;
import com.vocably.auth.User;
import com.vocably.auth.UserRepository;
import com.vocably.word.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReviewControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    WordRepository words;

    @Autowired
    CardRepository cards;

    User alice;
    User bob;

    @BeforeEach
    void setUp() {
        users.deleteAll(); // ON DELETE CASCADE no banco apaga palavras, cards e logs juntos
        alice = users.save(new User("alice@example.com", "hash", "Alice"));
        bob = users.save(new User("bob@example.com", "hash", "Bob"));
    }

    static RequestPostProcessor as(User user) {
        return jwt().jwt(token -> token.subject(user.getId().toString()));
    }

    @Test
    void creatingAWordCreatesTwoCardsDueToday() throws Exception {
        createWord(alice, "ephemeral", "efêmero");

        mvc.perform(get("/api/reviews/due").with(as(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].front", containsInAnyOrder("ephemeral", "efêmero")))
                .andExpect(jsonPath("$[*].back", containsInAnyOrder("efêmero", "ephemeral")));

        mvc.perform(get("/api/reviews/summary").with(as(alice)))
                .andExpect(jsonPath("$.dueToday").value(2))
                .andExpect(jsonPath("$.reviewedToday").value(0));
    }

    @Test
    void reviewingACardSchedulesItForLater() throws Exception {
        createWord(alice, "ephemeral", "efêmero");
        Long cardId = firstDueCardId(alice);

        mvc.perform(post("/api/reviews/{id}", cardId).with(as(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"grade": "GOOD"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervalDays").value(1))
                .andExpect(jsonPath("$.nextDueDate").value(LocalDate.now().plusDays(1).toString()));

        // O card revisado sai da fila de hoje e conta no resumo.
        mvc.perform(get("/api/reviews/summary").with(as(alice)))
                .andExpect(jsonPath("$.dueToday").value(1))
                .andExpect(jsonPath("$.reviewedToday").value(1));
    }

    @Test
    void rejectsInvalidGrade() throws Exception {
        createWord(alice, "ephemeral", "efêmero");
        Long cardId = firstDueCardId(alice);

        mvc.perform(post("/api/reviews/{id}", cardId).with(as(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"grade": "PERFECT"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void usersCannotReviewEachOthersCards() throws Exception {
        createWord(alice, "ephemeral", "efêmero");
        Long cardId = firstDueCardId(alice);

        mvc.perform(get("/api/reviews/due").with(as(bob)))
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(post("/api/reviews/{id}", cardId).with(as(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"grade": "GOOD"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingAWordDeletesItsCards() throws Exception {
        Long wordId = createWord(alice, "ephemeral", "efêmero");

        mvc.perform(delete("/api/words/{id}", wordId).with(as(alice)))
                .andExpect(status().isNoContent());

        assertThat(cards.count()).isZero();
    }

    private Long createWord(User user, String term, String translation) throws Exception {
        String body = mvc.perform(post("/api/words").with(as(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"term": "%s", "translation": "%s"}
                                """.formatted(term, translation)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private Long firstDueCardId(User user) throws Exception {
        String body = mvc.perform(get("/api/reviews/due").with(as(user)))
                .andReturn().getResponse().getContentAsString();
        List<Number> ids = JsonPath.read(body, "$[*].id");
        return ids.getFirst().longValue();
    }
}
