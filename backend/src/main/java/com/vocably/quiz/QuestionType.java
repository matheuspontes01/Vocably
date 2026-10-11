package com.vocably.quiz;

public enum QuestionType {
    /** "O que significa X?" → opções em português. */
    MEANING,
    /** "Como se diz X em inglês?" → opções em inglês. */
    REVERSE,
    /** Frase em inglês com uma lacuna → opções em inglês. */
    FILL_IN_THE_BLANK
}
