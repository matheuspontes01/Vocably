package com.vocably.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * O que o Claude devolve ao pesquisar uma palavra.
 *
 * <p>O SDK gera um JSON Schema a partir deste record e a API garante que a resposta segue
 * esse formato — então não precisamos "parsear texto livre". As descrições abaixo vão
 * junto no schema e orientam o modelo.
 */
public record WordLookup(
        @JsonPropertyDescription("Dictionary (base) form of the word or expression, e.g. 'run' for 'ran'. Keep phrasal verbs and idioms whole.")
        String term,
        @JsonPropertyDescription("Best Brazilian Portuguese translation for the meaning used in the context sentence (or the most common meaning when there is no sentence). One or two words when possible.")
        String translation,
        @JsonPropertyDescription("Up to 3 other common Brazilian Portuguese translations, most common first. Empty if none.")
        List<String> alternativeTranslations,
        @JsonPropertyDescription("Part of speech in Portuguese, e.g. 'substantivo', 'verbo', 'adjetivo', 'expressão'.")
        String partOfSpeech,
        @JsonPropertyDescription("IPA pronunciation of the base form, American English, between slashes, e.g. /ɪˈfem(ə)rəl/.")
        String phonetic,
        @JsonPropertyDescription("Short definition in simple English (max 15 words) for the meaning used in context.")
        String definition,
        @JsonPropertyDescription("A new, natural English example sentence (different from the context sentence) using the word with the same meaning.")
        String exampleSentence,
        @JsonPropertyDescription("Brazilian Portuguese translation of exampleSentence.")
        String exampleTranslation,
        @JsonPropertyDescription("One short tip in Portuguese for a Brazilian learner (false friend, common mistake, register, related word). Empty string if nothing useful.")
        String usageNote
) {
}
