package com.vocably.stats;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Tudo que a página de progresso mostra, em uma única resposta.
 *
 * @param accuracy fração (0 a 1) de respostas diferentes de "Errei" no período; null sem revisões
 */
public record StatsResponse(
        int days,
        long totalWords,
        int currentStreak,
        long reviewsInPeriod,
        Double accuracy,
        List<DailyCount> reviewsPerDay,
        List<DailyCount> totalWordsPerDay,
        Map<CardStage, Long> cardStages,
        Map<String, Long> grades
) {

    public record DailyCount(LocalDate date, long count) {
    }
}
