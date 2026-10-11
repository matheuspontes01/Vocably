package com.vocably.ai;

/** Uma palavra do usuário enviada ao Claude para gerar perguntas. */
public record QuizWord(long id, String term, String translation, String contextSentence) {
}
