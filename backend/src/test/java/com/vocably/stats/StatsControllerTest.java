package com.vocably.stats;

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

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class StatsControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    User alice;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        alice = users.save(new User("alice@example.com", "hash", "Alice"));
    }

    RequestPostProcessor asAlice() {
        return jwt().jwt(t -> t.subject(alice.getId().toString()));
    }

    @Test
    void summarizesWordsReviewsAndCards() throws Exception {
        mvc.perform(post("/api/words").with(asAlice()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"term\": \"bank\", \"translation\": \"margem\"}"));
        String due = mvc.perform(get("/api/reviews/due").with(asAlice())).andReturn().getResponse().getContentAsString();
        List<Integer> cardIds = com.jayway.jsonpath.JsonPath.read(due, "$[*].id");
        mvc.perform(post("/api/reviews/{id}", cardIds.get(0)).with(asAlice()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"grade\": \"GOOD\"}"));
        mvc.perform(post("/api/reviews/{id}", cardIds.get(1)).with(asAlice()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"grade\": \"AGAIN\"}"));

        mvc.perform(get("/api/stats").param("days", "7").with(asAlice()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWords").value(1))
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.reviewsInPeriod").value(2))
                .andExpect(jsonPath("$.accuracy").value(0.5))
                .andExpect(jsonPath("$.reviewsPerDay.length()").value(7))
                .andExpect(jsonPath("$.reviewsPerDay[6].count").value(2))
                .andExpect(jsonPath("$.totalWordsPerDay[6].count").value(1))
                .andExpect(jsonPath("$.cardStages.LEARNING").value(2))
                .andExpect(jsonPath("$.cardStages.NEW").value(0))
                .andExpect(jsonPath("$.grades.GOOD").value(1))
                .andExpect(jsonPath("$.grades.AGAIN").value(1));
    }

    @Test
    void worksForNewUser() throws Exception {
        mvc.perform(get("/api/stats").with(asAlice()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWords").value(0))
                .andExpect(jsonPath("$.accuracy").doesNotExist())
                .andExpect(jsonPath("$.reviewsPerDay.length()").value(30));
    }

    @Test
    void rejectsInvalidPeriod() throws Exception {
        mvc.perform(get("/api/stats").param("days", "1000").with(asAlice()))
                .andExpect(status().isBadRequest());
    }
}
