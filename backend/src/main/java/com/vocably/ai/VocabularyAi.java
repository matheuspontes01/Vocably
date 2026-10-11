package com.vocably.ai;

import java.util.List;

/**
 * O que a aplicação precisa da IA. Ser uma interface permite trocar a implementação
 * nos testes por um mock, sem chamar a API de verdade (que custa dinheiro e é lenta).
 */
public interface VocabularyAi {

    /** Tradução, definição, pronúncia e exemplo de uma palavra, considerando a frase onde ela apareceu. */
    WordLookup lookup(String term, String contextSentence);

    /** Uma pergunta de múltipla escolha para cada palavra. */
    List<GeneratedQuestion> generateQuiz(List<QuizWord> words);
}
