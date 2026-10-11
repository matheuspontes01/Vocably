package com.vocably.dictionary;

import java.util.List;

/** O que extraímos da resposta do Free Dictionary API. Campos podem vir null. */
public record DictionaryEntry(String phonetic, String audioUrl, List<Meaning> meanings) {

    /** Uma acepção: classe gramatical (em inglês, como a API devolve), definição e exemplo. */
    public record Meaning(String partOfSpeech, String definition, String example) {
    }
}
