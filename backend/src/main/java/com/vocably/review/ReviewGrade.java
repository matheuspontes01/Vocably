package com.vocably.review;

/**
 * Os quatro botões da tela de revisão. Cada um corresponde a uma nota de 0 a 5
 * do algoritmo SM-2 ("quality" no artigo original).
 */
public enum ReviewGrade {
    AGAIN(1),
    HARD(3),
    GOOD(4),
    EASY(5);

    private final int quality;

    ReviewGrade(int quality) {
        this.quality = quality;
    }

    public int quality() {
        return quality;
    }
}
