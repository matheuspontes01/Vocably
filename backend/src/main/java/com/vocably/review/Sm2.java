package com.vocably.review;

import java.time.LocalDate;

/**
 * Algoritmo SM-2 de repetição espaçada (o mesmo que deu origem ao Anki).
 *
 * <p>É um record imutável e sem dependências de Spring ou banco: recebe o estado atual
 * do card e a resposta do usuário, e devolve o próximo estado. Isso o torna fácil de testar.
 *
 * @param easeFactor   o quanto o intervalo cresce a cada acerto (começa em 2.5, mínimo 1.3)
 * @param intervalDays dias até a próxima revisão
 * @param repetitions  acertos seguidos
 */
public record Sm2(double easeFactor, int intervalDays, int repetitions) {

    static final double INITIAL_EASE = 2.5;
    static final double MIN_EASE = 1.3;

    public static Sm2 initial() {
        return new Sm2(INITIAL_EASE, 0, 0);
    }

    public Sm2 next(ReviewGrade grade) {
        int q = grade.quality();

        int reps;
        int interval;
        if (q < 3) {
            // Errou: volta para o começo e revisa amanhã.
            reps = 0;
            interval = 1;
        } else {
            reps = repetitions + 1;
            interval = switch (reps) {
                case 1 -> 1;
                case 2 -> 6;
                default -> (int) Math.round(intervalDays * easeFactor);
            };
        }

        // Fórmula original do SM-2: respostas fáceis aumentam o fator, difíceis diminuem.
        double ease = easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
        ease = Math.max(MIN_EASE, Math.round(ease * 100) / 100.0);

        return new Sm2(ease, interval, reps);
    }

    public LocalDate dueDateFrom(LocalDate today) {
        return today.plusDays(intervalDays);
    }
}
