package com.vocably.review;

/** Um card da fila de revisão, já com frente e verso montados. */
public record CardResponse(
        Long id,
        Long wordId,
        CardDirection direction,
        String front,
        String back,
        String contextSentence
) {

    static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(),
                card.getWord().getId(),
                card.getDirection(),
                card.front(),
                card.back(),
                card.getWord().getContextSentence()
        );
    }
}
