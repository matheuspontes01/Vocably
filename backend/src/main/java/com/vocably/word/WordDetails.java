package com.vocably.word;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Informações extras de uma palavra. É um {@code @Embeddable}: as colunas ficam na própria
 * tabela words, mas no Java agrupamos tudo num objeto só (um record imutável).
 */
@Embeddable
public record WordDetails(
        @Column(name = "part_of_speech", length = 50) String partOfSpeech,
        @Column(length = 100) String phonetic,
        String definition,
        @Column(name = "example_sentence") String exampleSentence,
        @Column(name = "example_translation") String exampleTranslation
) {

    public static final WordDetails EMPTY = new WordDetails(null, null, null, null, null);
}
