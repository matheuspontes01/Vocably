package com.vocably.word;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo das requisições de criação e edição de palavra. Os campos de detalhe são opcionais. */
public record WordRequest(
        @NotBlank @Size(max = 100) String term,
        @NotBlank @Size(max = 255) String translation,
        String contextSentence,
        SourceType sourceType,
        @Size(max = 255) String sourceTitle,
        @Size(max = 50) String partOfSpeech,
        @Size(max = 100) String phonetic,
        String definition,
        String exampleSentence,
        String exampleTranslation
) {

    /** Atalho para quem só tem os campos básicos (usado nos testes). */
    public WordRequest(String term, String translation, String contextSentence, SourceType sourceType, String sourceTitle) {
        this(term, translation, contextSentence, sourceType, sourceTitle, null, null, null, null, null);
    }

    WordDetails details() {
        return new WordDetails(blankToNull(partOfSpeech), blankToNull(phonetic), blankToNull(definition),
                blankToNull(exampleSentence), blankToNull(exampleTranslation));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
