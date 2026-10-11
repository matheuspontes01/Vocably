package com.vocably.dictionary;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Junta dicionário (pronúncia, definições) e tradução numa única sugestão. */
@Service
public class LookupService {

    private static final Map<String, String> PARTS_OF_SPEECH = Map.of(
            "noun", "substantivo",
            "verb", "verbo",
            "adjective", "adjetivo",
            "adverb", "advérbio",
            "pronoun", "pronome",
            "preposition", "preposição",
            "conjunction", "conjunção",
            "interjection", "interjeição",
            "phrase", "expressão");

    private final FreeDictionaryClient dictionary;
    private final MyMemoryTranslationClient translator;

    public LookupService(FreeDictionaryClient dictionary, MyMemoryTranslationClient translator) {
        this.dictionary = dictionary;
        this.translator = translator;
    }

    public WordLookup lookup(String rawTerm, String contextSentence) {
        // O dicionário só conhece palavras em minúsculas ("Bank" → "bank").
        String term = rawTerm.trim().toLowerCase(Locale.ROOT);

        Optional<DictionaryEntry> entry = dictionary.lookup(term);
        Optional<Translation> translation = translator.translate(term);
        Optional<Translation> context = contextSentence == null || contextSentence.isBlank()
                ? Optional.empty()
                : translator.translate(contextSentence.trim());

        List<WordLookup.Definition> definitions = entry.map(DictionaryEntry::meanings).orElse(List.of()).stream()
                .map(m -> new WordLookup.Definition(toPortuguese(m.partOfSpeech()), m.definition(), m.example()))
                .toList();

        return new WordLookup(
                term,
                translation.map(Translation::text).orElse(null),
                translation.map(Translation::alternatives).orElse(List.of()),
                entry.map(DictionaryEntry::phonetic).orElse(null),
                entry.map(DictionaryEntry::audioUrl).orElse(null),
                definitions,
                context.map(Translation::text).orElse(null));
    }

    static String toPortuguese(String partOfSpeech) {
        if (partOfSpeech == null) {
            return null;
        }
        return PARTS_OF_SPEECH.getOrDefault(partOfSpeech.toLowerCase(Locale.ROOT), partOfSpeech);
    }
}
