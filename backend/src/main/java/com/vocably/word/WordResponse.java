package com.vocably.word;

import java.time.Instant;

/** O que a API devolve — desacoplado da entidade JPA. */
public record WordResponse(
        Long id,
        String term,
        String translation,
        String contextSentence,
        SourceType sourceType,
        String sourceTitle,
        String partOfSpeech,
        String phonetic,
        String definition,
        String exampleSentence,
        String exampleTranslation,
        Instant createdAt
) {

    static WordResponse from(Word word) {
        WordDetails details = word.getDetails();
        return new WordResponse(
                word.getId(),
                word.getTerm(),
                word.getTranslation(),
                word.getContextSentence(),
                word.getSourceType(),
                word.getSourceTitle(),
                details.partOfSpeech(),
                details.phonetic(),
                details.definition(),
                details.exampleSentence(),
                details.exampleTranslation(),
                word.getCreatedAt()
        );
    }
}
