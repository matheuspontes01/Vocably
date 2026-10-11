package com.vocably.quiz;

import com.vocably.ai.QuestionType;

import java.util.List;

/**
 * Uma pergunta pronta para o frontend. A resposta certa vai junto para a correção ser
 * instantânea na tela — aceitável num app de estudo pessoal, onde "colar" não tem sentido.
 */
public record QuizQuestion(
        Long wordId,
        String term,
        QuestionType type,
        String prompt,
        List<String> options,
        int correctOption,
        String explanation
) {
}
