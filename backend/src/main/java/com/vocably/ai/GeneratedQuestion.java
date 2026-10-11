package com.vocably.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record GeneratedQuestion(
        @JsonPropertyDescription("id of the word this question practices (copy it from the input)")
        long wordId,
        QuestionType type,
        @JsonPropertyDescription("The question shown to the student. FILL_IN_THE_BLANK: an English sentence with the target word replaced by '_____'. MEANING_IN_CONTEXT: a Portuguese question like 'O que significa \"X\" na frase abaixo?' followed by the English sentence.")
        String prompt,
        @JsonPropertyDescription("Exactly 4 answer options with exactly one correct. FILL_IN_THE_BLANK: English words that fit grammatically. MEANING_IN_CONTEXT: Portuguese translations.")
        List<String> options,
        @JsonPropertyDescription("0-based index of the correct option in options")
        int correctOption,
        @JsonPropertyDescription("One or two sentences in Portuguese explaining why the answer is right")
        String explanation
) {
}
