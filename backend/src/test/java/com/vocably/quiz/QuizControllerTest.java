package com.vocably.quiz;

import com.vocably.TestcontainersConfiguration;
import com.vocably.ai.GeneratedQuestion;
import com.vocably.ai.QuestionType;
import com.vocably.ai.VocabularyAi;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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

    // Substitui a IA real por um mock: o teste não chama a API da Anthropic.
    @MockitoBean
    VocabularyAi ai;

    User alice;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        alice = users.save(new User("alice@example.com", "hash", "Alice"));
    }

    @Test
    void generatesQuizFromUsersWords() throws Exception {
        Word word = words.save(new Word(alice.getId(), "ephemeral", "efêmero", null, SourceType.OTHER, null));
        when(ai.generateQuiz(any())).thenReturn(List.of(new GeneratedQuestion(word.getId(),
                QuestionType.MEANING_IN_CONTEXT, "O que significa \"ephemeral\"?",
                List.of("efêmero", "eterno", "enorme", "minúsculo"), 0, "Ephemeral = que dura pouco.")));

        mvc.perform(post("/api/quizzes").with(jwt().jwt(t -> t.subject(alice.getId().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].term").value("ephemeral"))
                .andExpect(jsonPath("$[0].options.length()").value(4))
                .andExpect(jsonPath("$[0].type").value("MEANING_IN_CONTEXT"));
    }

    @Test
    void rejectsTooLargeQuiz() throws Exception {
        mvc.perform(post("/api/quizzes").param("size", "50")
                        .with(jwt().jwt(t -> t.subject(alice.getId().toString()))))
                .andExpect(status().isBadRequest());
    }
}
