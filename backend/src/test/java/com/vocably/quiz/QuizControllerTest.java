package com.vocably.quiz;

import com.vocably.TestcontainersConfiguration;
import com.vocably.auth.User;
import com.vocably.auth.UserRepository;
import com.vocably.word.SourceType;
import com.vocably.word.Word;
import com.vocably.word.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class QuizControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @Autowired
    WordRepository words;

    User alice;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        alice = users.save(new User("alice@example.com", "hash", "Alice"));
    }

    RequestPostProcessor asAlice() {
        return jwt().jwt(t -> t.subject(alice.getId().toString()));
    }

    void saveWords(String... pairs) {
        for (int i = 0; i < pairs.length; i += 2) {
            words.save(new Word(alice.getId(), pairs[i], pairs[i + 1], null, SourceType.OTHER, null));
        }
    }

    @Test
    void generatesOneQuestionPerWord() throws Exception {
        saveWords("ephemeral", "efêmero", "bank", "margem", "walk", "caminhar", "bright", "brilhante", "huge", "enorme");

        mvc.perform(post("/api/quizzes").param("size", "3").with(asAlice()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].options.length()").value(4))
                .andExpect(jsonPath("$[0].correctOption").isNumber());
    }

    @Test
    void requiresAtLeastFourWords() throws Exception {
        saveWords("ephemeral", "efêmero", "bank", "margem", "walk", "caminhar");

        mvc.perform(post("/api/quizzes").with(asAlice()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Save at least 4 words to take a quiz"));
    }
}
