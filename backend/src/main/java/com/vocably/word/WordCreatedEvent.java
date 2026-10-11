package com.vocably.word;

/** Publicado quando uma palavra é cadastrada. Outros módulos (ex.: revisão) reagem a ele. */
public record WordCreatedEvent(Long wordId, Long userId) {
}
