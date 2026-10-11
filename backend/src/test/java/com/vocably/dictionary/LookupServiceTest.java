package com.vocably.dictionary;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LookupServiceTest {

    FreeDictionaryClient dictionary = mock(FreeDictionaryClient.class);
    MyMemoryTranslationClient translator = mock(MyMemoryTranslationClient.class);
    LookupService service = new LookupService(dictionary, translator);

    @Test
    void combinesDictionaryAndTranslationInPortuguese() {
        when(dictionary.lookup("bank")).thenReturn(Optional.of(new DictionaryEntry("/bæŋk/", "https://audio/bank-us.mp3",
                List.of(new DictionaryEntry.Meaning("noun", "The edge of a river.", null)))));
        when(translator.translate("bank")).thenReturn(Optional.of(new Translation("banco", List.of("margem"))));
        when(translator.translate("We sat on the bank.")).thenReturn(Optional.of(new Translation("Sentamos na margem.", List.of())));

        WordLookup lookup = service.lookup("  Bank ", "We sat on the bank.");

        assertThat(lookup.term()).isEqualTo("bank");
        assertThat(lookup.translation()).isEqualTo("banco");
        assertThat(lookup.alternativeTranslations()).containsExactly("margem");
        assertThat(lookup.phonetic()).isEqualTo("/bæŋk/");
        assertThat(lookup.definitions().getFirst().partOfSpeech()).isEqualTo("substantivo");
        assertThat(lookup.contextTranslation()).isEqualTo("Sentamos na margem.");
    }

    @Test
    void worksWhenBothApisFail() {
        when(dictionary.lookup("bank")).thenReturn(Optional.empty());
        when(translator.translate("bank")).thenReturn(Optional.empty());

        WordLookup lookup = service.lookup("bank", null);

        assertThat(lookup.translation()).isNull();
        assertThat(lookup.definitions()).isEmpty();
        assertThat(lookup.contextTranslation()).isNull();
    }

    @Test
    void keepsUnknownPartOfSpeechInEnglish() {
        assertThat(LookupService.toPortuguese("Verb")).isEqualTo("verbo");
        assertThat(LookupService.toPortuguese("abbreviation")).isEqualTo("abbreviation");
    }
}
