package com.vocably.stats;

import com.vocably.review.CardSchedule;
import com.vocably.review.ReviewGrade;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Regras das estatísticas, em Java puro (sem banco): recebe listas e devolve números.
 * Assim cada regra — sequência de dias, estágio dos cards — tem teste unitário simples.
 */
final class StatsCalculator {

    static final int YOUNG_FROM_DAYS = 7;
    static final int MATURE_FROM_DAYS = 21;

    private StatsCalculator() {
    }

    static CardStage stageOf(CardSchedule card) {
        if (card.lastReviewedAt() == null) {
            return CardStage.NEW;
        }
        if (card.intervalDays() >= MATURE_FROM_DAYS) {
            return CardStage.MATURE;
        }
        return card.intervalDays() >= YOUNG_FROM_DAYS ? CardStage.YOUNG : CardStage.LEARNING;
    }

    /** Sempre devolve os 4 estágios, mesmo com zero, na ordem do enum. */
    static Map<CardStage, Long> countStages(Collection<CardSchedule> cards) {
        Map<CardStage, Long> counts = new EnumMap<>(CardStage.class);
        for (CardStage stage : CardStage.values()) {
            counts.put(stage, 0L);
        }
        cards.forEach(card -> counts.merge(stageOf(card), 1L, Long::sum));
        return counts;
    }

    /** Sempre devolve as 4 notas, na ordem dos botões (Errei → Fácil). */
    static Map<String, Long> countGrades(Collection<ReviewGrade> grades) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ReviewGrade grade : ReviewGrade.values()) {
            counts.put(grade.name(), 0L);
        }
        grades.forEach(grade -> counts.merge(grade.name(), 1L, Long::sum));
        return counts;
    }

    /** Fração de respostas que não foram "Errei"; null quando não houve revisão. */
    static Double accuracy(Collection<ReviewGrade> grades) {
        if (grades.isEmpty()) {
            return null;
        }
        long correct = grades.stream().filter(g -> g != ReviewGrade.AGAIN).count();
        return (double) correct / grades.size();
    }

    /**
     * Uma linha por dia de {@code from} até {@code to}, inclusive — dias sem atividade
     * entram com zero, senão o gráfico "pularia" esses dias.
     */
    static List<StatsResponse.DailyCount> perDay(Collection<LocalDate> events, LocalDate from, LocalDate to) {
        Map<LocalDate, Long> counts = events.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        List<StatsResponse.DailyCount> days = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            days.add(new StatsResponse.DailyCount(day, counts.getOrDefault(day, 0L)));
        }
        return days;
    }

    /** Total acumulado dia a dia, começando do que já existia antes do período. */
    static List<StatsResponse.DailyCount> cumulative(List<StatsResponse.DailyCount> perDay, long startingTotal) {
        List<StatsResponse.DailyCount> result = new ArrayList<>();
        long total = startingTotal;
        for (StatsResponse.DailyCount day : perDay) {
            total += day.count();
            result.add(new StatsResponse.DailyCount(day.date(), total));
        }
        return result;
    }

    /**
     * Dias seguidos com pelo menos uma revisão, terminando hoje. Se ainda não revisou hoje,
     * conta a partir de ontem: a sequência só "quebra" quando um dia inteiro passa em branco.
     */
    static int streak(Set<LocalDate> activeDays, LocalDate today) {
        LocalDate day = activeDays.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (activeDays.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }
}
