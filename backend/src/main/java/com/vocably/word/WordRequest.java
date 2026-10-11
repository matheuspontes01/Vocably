package com.vocably.word;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo das requisições de criação e edição de palavra. */
public record WordRequest(
        @NotBlank @Size(max = 100) String term,
        @NotBlank @Size(max = 255) String translation,
        String contextSentence,
        SourceType sourceType,
        @Size(max = 255) String sourceTitle
) {
}
