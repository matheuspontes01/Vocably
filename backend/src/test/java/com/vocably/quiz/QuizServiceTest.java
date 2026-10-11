package com.vocably.quiz;

import com.vocably.ai.GeneratedQuestion;
import com.vocably.ai.QuestionType;
import com.vocably.ai.VocabularyAi;
import com.vocably.word.SourceType;
import com.vocably.word.Word;
import com.vocably.word.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuizServiceTest {

    static final Long USER_ID = 1L;

    WordRepository words = mock(WordRepository.class);
    VocabularyAi ai = mock(VocabularyAi.class);
    QuizService service = new QuizService(words, ai, new Random(42));

    Word ephemeral;

    @BeforeEach
    void setUp() {
        ephemeral = new Word(USER_ID, "ephemeral", "efêmero", null, SourceType.OTHER, null);
        // O id normalmente vem do banco; no teste unitário colocamos na mão.
        ReflectionTestUtils.setField(ephemeral, "id", 10L);
        when(words.findRandom(USER_ID, 5)).thenReturn(List.of(ephemeral));
    }

    @Test
    void keepsTheCorrectAnswerAfterShufflingOptions() {
        when(ai.generateQuiz(any())).thenReturn(List.of(question(10L, List.of("ephemeral", "eternal", "huge", "tiny"), 0)));

        List<QuizQuestion> quiz = service.generate(USER_ID, 5);

        assertThat(quiz).hasSize(1);
        QuizQuestion q = quiz.getFirst();
        assertThat(q.options()).containsExactlyInAnyOrder("ephemeral", "eternal", "huge", "tiny");
        assertThat(q.options().get(q.correctOption())).isEqualTo("ephemeral");
        assertThat(q.term()).isEqualTo("ephemeral");
    }

    @Test
    void discardsMalformedQuestions() {
        when(ai.generateQuiz(any())).thenReturn(List.of(
                question(10L, List.of("a", "b", "c"), 0),            // só 3 opções
                question(10L, List.of("a", "b", "c", "d"), 4),       // índice fora do intervalo
                question(10L, List.of("a", "a", "c", "d"), 0),       // opções repetidas
                question(99L, List.of("a", "b", "c", "d"), 0),       // palavra que não foi pedida
                question(10L, List.of("a", "b", "c", "d"), 2)));     // válida

        assertThat(service.generate(USER_ID, 5)).hasSize(1);
    }

    @Test
    void returnsEmptyQuizWithoutCallingAiWhenUserHasNoWords() {
        when(words.findRandom(USER_ID, 5)).thenReturn(List.of());

        assertThat(service.generate(USER_ID, 5)).isEmpty();
        verify(ai, never()).generateQuiz(any());
    }

    static GeneratedQuestion question(long wordId, List<String> options, int correct) {
        return new GeneratedQuestion(wordId, QuestionType.FILL_IN_THE_BLANK, "Fame is _____.", options, correct, "Explicação.");
    }
}
