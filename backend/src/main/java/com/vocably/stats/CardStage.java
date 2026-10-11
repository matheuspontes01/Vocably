package com.vocably.stats;

/** Estágio de um card, pelo intervalo atual do SM-2 (convenção parecida com a do Anki). */
public enum CardStage {
    /** Nunca revisado. */
    NEW,
    /** Revisado, mas volta em menos de 7 dias. */
    LEARNING,
    /** Intervalo de 7 a 20 dias. */
    YOUNG,
    /** Intervalo de 21 dias ou mais: a palavra está bem fixada. */
    MATURE
}
