package com.vocably.word;

import com.vocably.TestcontainersConfiguration;
import com.vocably.ai.AiUnavailableException;
import com.vocably.ai.VocabularyAi;
import com.vocably.ai.WordLookup;
import com.vocably.auth.User;
import com.vocably.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WordLookupControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    @MockitoBean
    VocabularyAi ai;

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
    void returnsAiSuggestion() throws Exception {
        when(ai.lookup("bank", "We sat on the bank of the river.")).thenReturn(new WordLookup(
                "bank", "margem", List.of("banco"), "substantivo", "/bæŋk/", "the land along a river",
                "They walked along the river bank.", "Eles caminharam pela margem do rio.", ""));

        mvc.perform(get("/api/words/lookup").with(asAlice())
                        .param("term", "bank")
                        .param("context", "We sat on the bank of the river."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestion.translation").value("margem"))
                .andExpect(jsonPath("$.suggestion.alternativeTranslations[0]").value("banco"))
                .andExpect(jsonPath("$.existingWordId").doesNotExist());
    }

    @Test
    void returns503WhenAiIsUnavailable() throws Exception {
        when(ai.lookup("bank", null)).thenThrow(new AiUnavailableException("AI features are disabled"));

        mvc.perform(get("/api/words/lookup").with(asAlice()).param("term", "bank"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("AI features are disabled"));
    }

    @Test
    void savesAndReturnsWordDetails() throws Exception {
        mvc.perform(post("/api/words").with(asAlice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"term": "bank", "translation": "margem", "partOfSpeech": "substantivo",
                                 "phonetic": "/bæŋk/", "definition": "the land along a river",
                                 "exampleSentence": "They walked along the river bank.",
                                 "exampleTranslation": "Eles caminharam pela margem do rio."}
                                """))
                .andExpect(status().isCreated());

        // Os detalhes aparecem no verso do flashcard.
        mvc.perform(get("/api/reviews/due").with(asAlice()))
                .andExpect(jsonPath("$[0].phonetic").value("/bæŋk/"))
                .andExpect(jsonPath("$[0].definition").value("the land along a river"))
                .andExpect(jsonPath("$[0].exampleSentence").value("They walked along the river bank."));
    }
}
