package com.vocably.quiz;

import com.vocably.word.SourceType;
import com.vocably.word.Word;
import com.vocably.word.WordDetails;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class QuizGeneratorTest {

    QuizGenerator generator = new QuizGenerator(new Random(42));

    static Word word(long id, String term, String translation, String context) {
        Word word = new Word(1L, term, translation, context, SourceType.OTHER, null);
        ReflectionTestUtils.setField(word, "id", id); // o id normalmente vem do banco
        return word;
    }

    final Word ephemeral = word(1, "ephemeral", "efêmero", "Fame is ephemeral.");
    final Word bank = word(2, "bank", "margem", null);
    final Word walk = word(3, "walk", "caminhar", null);
    final Word bright = word(4, "bright", "brilhante", null);
    final Word huge = word(5, "huge", "enorme", null);
    final List<Word> pool = List.of(ephemeral, bank, walk, bright, huge);

    @Test
    void prefersFillInTheBlankWhenSentenceContainsTheWord() {
        QuizQuestion q = generator.question(ephemeral, pool).orElseThrow();

        assertThat(q.type()).isEqualTo(QuestionType.FILL_IN_THE_BLANK);
        assertThat(q.sentence()).isEqualTo("Fame is _____.");
        assertThat(q.options().get(q.correctOption())).isEqualTo("ephemeral");
    }

    @Test
    void everyQuestionHasFourDistinctOptionsWithTheRightAnswer() {
        for (Word target : pool) {
            QuizQuestion q = generator.question(target, pool).orElseThrow();

            assertThat(q.options()).hasSize(4).doesNotHaveDuplicates();
            String expected = q.type() == QuestionType.MEANING ? target.getTranslation() : target.getTerm();
            assertThat(q.options().get(q.correctOption())).isEqualTo(expected);
        }
    }

    @Test
    void usesExampleSentenceWhenThereIsNoContext() {
        Word withExample = new Word(1L, "walk", "caminhar", null, SourceType.OTHER, null,
                new WordDetails(null, null, null, null, "They walked to school."));
        ReflectionTestUtils.setField(withExample, "id", 9L);

        QuizQuestion q = generator.question(withExample, pool).orElseThrow();

        assertThat(q.type()).isEqualTo(QuestionType.FILL_IN_THE_BLANK);
        assertThat(q.sentence()).isEqualTo("They _____ to school.");
    }

    @Test
    void skipsWordWhenThereAreNotEnoughDistractors() {
        assertThat(generator.question(bank, List.of(bank, walk, bright))).isEmpty();
    }

    @Test
    void blanksSimpleInflectionsButNotOtherWords() {
        assertThat(QuizGenerator.sentenceWithBlank("bank", "Two banks closed.")).contains("Two _____ closed.");
        assertThat(QuizGenerator.sentenceWithBlank("walk", "She is WALKING home.")).contains("She is _____ home.");
        assertThat(QuizGenerator.sentenceWithBlank("art", "I read an article.")).isEmpty();
        assertThat(QuizGenerator.sentenceWithBlank("bank", null)).isEmpty();
    }
}
