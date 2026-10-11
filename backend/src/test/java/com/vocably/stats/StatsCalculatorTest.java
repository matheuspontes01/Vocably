package com.vocably.stats;

import com.vocably.review.CardSchedule;
import com.vocably.review.ReviewGrade;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StatsCalculatorTest {

    static final LocalDate TODAY = LocalDate.of(2026, 10, 11);
    static final Instant REVIEWED = Instant.parse("2026-10-01T10:00:00Z");

    @Test
    void classifiesCardsByInterval() {
        assertThat(StatsCalculator.stageOf(new CardSchedule(0, null))).isEqualTo(CardStage.NEW);
        assertThat(StatsCalculator.stageOf(new CardSchedule(1, REVIEWED))).isEqualTo(CardStage.LEARNING);
        assertThat(StatsCalculator.stageOf(new CardSchedule(6, REVIEWED))).isEqualTo(CardStage.LEARNING);
        assertThat(StatsCalculator.stageOf(new CardSchedule(7, REVIEWED))).isEqualTo(CardStage.YOUNG);
        assertThat(StatsCalculator.stageOf(new CardSchedule(21, REVIEWED))).isEqualTo(CardStage.MATURE);
    }

    @Test
    void countsEveryStageEvenWhenZero() {
        var counts = StatsCalculator.countStages(List.of(new CardSchedule(0, null), new CardSchedule(30, REVIEWED)));

        assertThat(counts).containsExactly(
                java.util.Map.entry(CardStage.NEW, 1L),
                java.util.Map.entry(CardStage.LEARNING, 0L),
                java.util.Map.entry(CardStage.YOUNG, 0L),
                java.util.Map.entry(CardStage.MATURE, 1L));
    }

    @Test
    void accuracyIgnoresOnlyAgain() {
        assertThat(StatsCalculator.accuracy(List.of(ReviewGrade.AGAIN, ReviewGrade.HARD, ReviewGrade.GOOD, ReviewGrade.EASY)))
                .isEqualTo(0.75);
        assertThat(StatsCalculator.accuracy(List.of())).isNull();
    }

    @Test
    void fillsDaysWithoutActivityWithZero() {
        var days = StatsCalculator.perDay(
                List.of(TODAY, TODAY, TODAY.minusDays(2)), TODAY.minusDays(3), TODAY);

        assertThat(days).extracting(StatsResponse.DailyCount::count).containsExactly(0L, 1L, 0L, 2L);
        assertThat(days.getFirst().date()).isEqualTo(TODAY.minusDays(3));
    }

    @Test
    void cumulativeStartsFromPreviousTotal() {
        var perDay = StatsCalculator.perDay(List.of(TODAY, TODAY.minusDays(1)), TODAY.minusDays(2), TODAY);

        assertThat(StatsCalculator.cumulative(perDay, 10))
                .extracting(StatsResponse.DailyCount::count).containsExactly(10L, 11L, 12L);
    }

    @Test
    void streakCountsConsecutiveDaysEndingToday() {
        Set<LocalDate> active = Set.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2), TODAY.minusDays(4));

        assertThat(StatsCalculator.streak(active, TODAY)).isEqualTo(3);
    }

    @Test
    void streakIsNotBrokenBeforeReviewingToday() {
        assertThat(StatsCalculator.streak(Set.of(TODAY.minusDays(1), TODAY.minusDays(2)), TODAY)).isEqualTo(2);
        assertThat(StatsCalculator.streak(Set.of(TODAY.minusDays(2)), TODAY)).isZero();
    }
}
