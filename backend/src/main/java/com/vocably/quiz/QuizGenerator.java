package com.vocably.quiz;

import com.vocably.word.Word;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Monta perguntas de múltipla escolha só com as palavras do usuário — sem IA.
 * As alternativas erradas ("distratores") são traduções ou termos das outras palavras salvas.
 *
 * <p>Não é um bean do Spring: é uma classe Java comum, fácil de testar com um {@link Random}
 * de semente fixa para o resultado ser sempre o mesmo.
 */
public class QuizGenerator {

    static final int OPTIONS = 4;
    static final String BLANK = "_____";

    private final Random random;

    public QuizGenerator(Random random) {
        this.random = random;
    }

    /**
     * @param targets palavras que viram perguntas
     * @param pool    todas as palavras disponíveis para servir de alternativa errada
     */
    public List<QuizQuestion> generate(List<Word> targets, List<Word> pool) {
        List<QuizQuestion> questions = new ArrayList<>();
        for (Word word : targets) {
            question(word, pool).ifPresent(questions::add);
        }
        return questions;
    }

    /** Prefere completar a frase (exercita o uso real); senão sorteia entre significado e tradução reversa. */
    Optional<QuizQuestion> question(Word word, List<Word> pool) {
        List<QuestionType> order = new ArrayList<>(List.of(QuestionType.MEANING, QuestionType.REVERSE));
        Collections.shuffle(order, random);
        order.addFirst(QuestionType.FILL_IN_THE_BLANK);

        for (QuestionType type : order) {
            Optional<QuizQuestion> question = switch (type) {
                case FILL_IN_THE_BLANK -> fillInTheBlank(word, pool);
                case MEANING -> meaning(word, pool);
                case REVERSE -> reverse(word, pool);
            };
            if (question.isPresent()) {
                return question;
            }
        }
        return Optional.empty();
    }

    private Optional<QuizQuestion> meaning(Word word, List<Word> pool) {
        return options(word.getTranslation(), pool, Word::getTranslation).map(options -> new QuizQuestion(
                word.getId(), word.getTerm(), QuestionType.MEANING,
                "O que significa “" + word.getTerm() + "”?",
                word.getContextSentence(),
                options, options.indexOf(word.getTranslation()),
                "“" + word.getTerm() + "” = " + word.getTranslation() + "."));
    }

    private Optional<QuizQuestion> reverse(Word word, List<Word> pool) {
        return options(word.getTerm(), pool, Word::getTerm).map(options -> new QuizQuestion(
                word.getId(), word.getTerm(), QuestionType.REVERSE,
                "Como se diz “" + word.getTranslation() + "” em inglês?",
                null,
                options, options.indexOf(word.getTerm()),
                "“" + word.getTranslation() + "” = " + word.getTerm() + "."));
    }

    private Optional<QuizQuestion> fillInTheBlank(Word word, List<Word> pool) {
        Optional<String> sentence = sentenceWithBlank(word.getTerm(), word.getContextSentence())
                .or(() -> sentenceWithBlank(word.getTerm(), word.getDetails().exampleSentence()));
        if (sentence.isEmpty()) {
            return Optional.empty();
        }
        return options(word.getTerm(), pool, Word::getTerm).map(options -> new QuizQuestion(
                word.getId(), word.getTerm(), QuestionType.FILL_IN_THE_BLANK,
                "Qual palavra completa a frase?",
                sentence.get(),
                options, options.indexOf(word.getTerm()),
                "“" + word.getTerm() + "” = " + word.getTranslation() + "."));
    }

    /**
     * Troca a palavra (e formas simples como "banks", "walked", "walking") por uma lacuna.
     * Vazio se a frase não contém a palavra.
     */
    static Optional<String> sentenceWithBlank(String term, String sentence) {
        if (sentence == null || sentence.isBlank()) {
            return Optional.empty();
        }
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(term) + "(s|es|d|ed|ing)?\\b",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(sentence);
        return matcher.find() ? Optional.of(matcher.replaceFirst(BLANK)) : Optional.empty();
    }

    /**
     * A resposta certa + 3 valores diferentes tirados de outras palavras, embaralhados.
     * Vazio se não houver alternativas suficientes.
     */
    private Optional<List<String>> options(String correct, List<Word> pool, Function<Word, String> field) {
        Set<String> seen = new LinkedHashSet<>();
        seen.add(correct.toLowerCase(Locale.ROOT));

        List<String> candidates = new ArrayList<>();
        for (Word other : pool) {
            String value = field.apply(other);
            if (value != null && !value.isBlank() && seen.add(value.toLowerCase(Locale.ROOT))) {
                candidates.add(value);
            }
        }
        if (candidates.size() < OPTIONS - 1) {
            return Optional.empty();
        }

        Collections.shuffle(candidates, random);
        List<String> options = new ArrayList<>(candidates.subList(0, OPTIONS - 1));
        options.add(correct);
        Collections.shuffle(options, random);
        return Optional.of(List.copyOf(options));
    }
}
