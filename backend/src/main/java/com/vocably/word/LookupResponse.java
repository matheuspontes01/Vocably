package com.vocably.word;

import com.vocably.dictionary.WordLookup;

/**
 * @param suggestion     o que o dicionário e o tradutor encontraram
 * @param existingWordId id da palavra se o usuário já a salvou antes (senão null)
 */
public record LookupResponse(WordLookup suggestion, Long existingWordId) {
}
