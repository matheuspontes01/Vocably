package com.vocably.dictionary;

import java.util.List;

/**
 * Sugestão para preencher o cadastro de uma palavra. Qualquer campo pode vir null/vazio
 * quando as APIs gratuitas não tiverem a informação.
 *
 * @param definitions        acepções do dicionário; o usuário escolhe a que combina com a frase
 * @param contextTranslation tradução automática da frase onde a palavra apareceu
 */
public record WordLookup(
        String term,
        String translation,
        List<String> alternativeTranslations,
        String phonetic,
        String audioUrl,
        List<Definition> definitions,
        String contextTranslation
) {

    /** @param partOfSpeech já em português ("substantivo", "verbo"...) */
    public record Definition(String partOfSpeech, String definition, String example) {
    }
}
