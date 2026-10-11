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
        Instant createdAt
) {

    static WordResponse from(Word word) {
        return new WordResponse(
                word.getId(),
                word.getTerm(),
                word.getTranslation(),
                word.getContextSentence(),
                word.getSourceType(),
                word.getSourceTitle(),
                word.getCreatedAt()
        );
    }
}
