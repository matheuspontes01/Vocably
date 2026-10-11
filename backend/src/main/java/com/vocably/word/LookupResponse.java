package com.vocably.word;

import com.vocably.ai.WordLookup;

/**
 * @param suggestion     o que a IA sugeriu
 * @param existingWordId id da palavra se o usuário já a salvou antes (senão null)
 */
public record LookupResponse(WordLookup suggestion, Long existingWordId) {
}
