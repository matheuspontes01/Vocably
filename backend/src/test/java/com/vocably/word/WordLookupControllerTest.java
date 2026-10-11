package com.vocably.word;

import com.vocably.TestcontainersConfiguration;
import com.vocably.auth.User;
import com.vocably.auth.UserRepository;
import com.vocably.dictionary.LookupService;
import com.vocably.dictionary.WordLookup;
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

    @Autowired
    WordRepository words;

    // Substitui o serviço real por um mock: o teste não acessa as APIs externas.
    @MockitoBean
    LookupService lookupService;

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
    void returnsSuggestionAndFlagsDuplicates() throws Exception {
        Word existing = words.save(new Word(alice.getId(), "bank", "banco", null, SourceType.OTHER, null));
        when(lookupService.lookup("bank", "We sat on the bank.")).thenReturn(new WordLookup(
                "bank", "banco", List.of("margem"), "/bæŋk/", "https://audio/bank-us.mp3",
                List.of(new WordLookup.Definition("substantivo", "The edge of a river.", null)), "Sentamos na margem."));

        mvc.perform(get("/api/words/lookup").with(asAlice())
                        .param("term", "bank")
                        .param("context", "We sat on the bank."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestion.translation").value("banco"))
                .andExpect(jsonPath("$.suggestion.definitions[0].partOfSpeech").value("substantivo"))
                .andExpect(jsonPath("$.suggestion.contextTranslation").value("Sentamos na margem."))
                .andExpect(jsonPath("$.existingWordId").value(existing.getId()));
    }

    @Test
    void savesAndReturnsWordDetailsOnCards() throws Exception {
        mvc.perform(post("/api/words").with(asAlice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"term": "bank", "translation": "margem", "partOfSpeech": "substantivo",
                                 "phonetic": "/bæŋk/", "audioUrl": "https://audio/bank-us.mp3",
                                 "definition": "The edge of a river.", "exampleSentence": "They walked along the bank."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.audioUrl").value("https://audio/bank-us.mp3"));

        // Os detalhes aparecem no verso do flashcard.
        mvc.perform(get("/api/reviews/due").with(asAlice()))
                .andExpect(jsonPath("$[0].phonetic").value("/bæŋk/"))
                .andExpect(jsonPath("$[0].definition").value("The edge of a river."))
                .andExpect(jsonPath("$[0].audioUrl").value("https://audio/bank-us.mp3"));
    }
}
