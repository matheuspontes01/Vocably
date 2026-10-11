package com.vocably.quiz;

import com.vocably.ai.GeneratedQuestion;
import com.vocably.ai.QuizWord;
import com.vocably.ai.VocabularyAi;
import com.vocably.word.Word;
import com.vocably.word.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuizService {

    static final int OPTIONS_PER_QUESTION = 4;

    private final WordRepository words;
    private final VocabularyAi ai;
    private final Random random;

    // Com mais de um construtor, o @Autowired diz ao Spring qual deles usar.
    @Autowired
    public QuizService(WordRepository words, VocabularyAi ai) {
        this(words, ai, new Random());
    }

    // Construtor extra para os testes passarem um Random com semente fixa (resultado previsível).
    QuizService(WordRepository words, VocabularyAi ai, Random random) {
        this.words = words;
        this.ai = ai;
        this.random = random;
    }

    @Transactional(readOnly = true)
    public List<QuizQuestion> generate(Long userId, int size) {
        List<Word> selected = words.findRandom(userId, size);
        if (selected.isEmpty()) {
            return List.of();
        }

        Map<Long, Word> byId = selected.stream().collect(Collectors.toMap(Word::getId, Function.identity()));
        List<QuizWord> input = selected.stream()
                .map(w -> new QuizWord(w.getId(), w.getTerm(), w.getTranslation(), w.getContextSentence()))
                .toList();

        // A IA pode errar o formato de alguma pergunta; descartamos as inválidas em vez de quebrar o quiz.
        return ai.generateQuiz(input).stream()
                .filter(q -> isValid(q, byId))
                .map(q -> shuffleOptions(q, byId.get(q.wordId())))
                .toList();
    }

    static boolean isValid(GeneratedQuestion q, Map<Long, Word> allowedWords) {
        return allowedWords.containsKey(q.wordId())
                && q.prompt() != null && !q.prompt().isBlank()
                && q.options() != null && q.options().size() == OPTIONS_PER_QUESTION
                && new HashSet<>(q.options()).size() == OPTIONS_PER_QUESTION // sem opções repetidas
                && q.correctOption() >= 0 && q.correctOption() < OPTIONS_PER_QUESTION;
    }

    /** Modelos de linguagem tendem a colocar a resposta certa sempre na mesma posição; embaralhamos. */
    private QuizQuestion shuffleOptions(GeneratedQuestion q, Word word) {
        String correct = q.options().get(q.correctOption());
        List<String> options = new ArrayList<>(q.options());
        Collections.shuffle(options, random);
        return new QuizQuestion(word.getId(), word.getTerm(), q.type(), q.prompt(), List.copyOf(options),
                options.indexOf(correct), q.explanation());
    }
}
