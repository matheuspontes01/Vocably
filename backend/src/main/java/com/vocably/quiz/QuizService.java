package com.vocably.quiz;

import com.vocably.shared.BusinessRuleException;
import com.vocably.word.Word;
import com.vocably.word.WordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
public class QuizService {

    /** 1 resposta certa + 3 erradas: abaixo disso não dá para montar alternativas. */
    static final int MIN_WORDS = QuizGenerator.OPTIONS;
    /** Quantas palavras sorteamos para servir de alternativas erradas. */
    static final int POOL_SIZE = 50;

    private final WordRepository words;
    private final QuizGenerator generator = new QuizGenerator(new Random());

    public QuizService(WordRepository words) {
        this.words = words;
    }

    @Transactional(readOnly = true)
    public List<QuizQuestion> generate(Long userId, int size) {
        if (words.countByUserId(userId) < MIN_WORDS) {
            throw new BusinessRuleException("Save at least " + MIN_WORDS + " words to take a quiz");
        }
        // Sorteio único: as primeiras viram perguntas, todas servem de alternativas.
        List<Word> pool = words.findRandom(userId, Math.max(size, POOL_SIZE));
        return generator.generate(pool.subList(0, Math.min(size, pool.size())), pool);
    }
}
