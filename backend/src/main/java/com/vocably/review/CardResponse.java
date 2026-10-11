package com.vocably.review;

import com.vocably.word.WordDetails;

/** Um card da fila de revisão, já com frente e verso montados. */
public record CardResponse(
        Long id,
        Long wordId,
        CardDirection direction,
        String front,
        String back,
        String contextSentence,
        String term,
        String partOfSpeech,
        String phonetic,
        String definition,
        String exampleSentence,
        String exampleTranslation
) {

    static CardResponse from(Card card) {
        var word = card.getWord();
        WordDetails details = word.getDetails();
        return new CardResponse(
                card.getId(),
                word.getId(),
                card.getDirection(),
                card.front(),
                card.back(),
                word.getContextSentence(),
                word.getTerm(),
                details.partOfSpeech(),
                details.phonetic(),
                details.definition(),
                details.exampleSentence(),
                details.exampleTranslation()
        );
    }
}
