package com.vocably.quiz;

import java.util.List;

/**
 * Uma pergunta de múltipla escolha. A resposta certa vai junto para a correção ser instantânea
 * na tela — aceitável num app de estudo pessoal, onde "colar" não tem sentido.
 *
 * @param sentence frase de apoio (a frase com lacuna, ou a frase onde a palavra apareceu); pode ser null
 */
public record QuizQuestion(
        Long wordId,
        String term,
        QuestionType type,
        String prompt,
        String sentence,
        List<String> options,
        int correctOption,
        String explanation
) {
}
