package com.vocably.review;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.vocably.review.ReviewGrade.AGAIN;
import static com.vocably.review.ReviewGrade.EASY;
import static com.vocably.review.ReviewGrade.GOOD;
import static com.vocably.review.ReviewGrade.HARD;
import static org.assertj.core.api.Assertions.assertThat;

class Sm2Test {

    @Test
    void intervalsGrowOneThenSixThenByEaseFactor() {
        Sm2 first = Sm2.initial().next(GOOD);
        Sm2 second = first.next(GOOD);
        Sm2 third = second.next(GOOD);

        assertThat(first.intervalDays()).isEqualTo(1);
        assertThat(second.intervalDays()).isEqualTo(6);
        // GOOD mantém o fator em 2.5 → 6 × 2.5 = 15
        assertThat(third.intervalDays()).isEqualTo(15);
        assertThat(third.repetitions()).isEqualTo(3);
    }

    @Test
    void forgettingResetsRepetitionsAndSchedulesForTomorrow() {
        Sm2 learned = new Sm2(2.5, 15, 3);

        Sm2 forgotten = learned.next(AGAIN);

        assertThat(forgotten.repetitions()).isZero();
        assertThat(forgotten.intervalDays()).isEqualTo(1);
        assertThat(forgotten.easeFactor()).isLessThan(2.5);
    }

    @Test
    void easyIncreasesAndHardDecreasesEaseFactor() {
        assertThat(Sm2.initial().next(EASY).easeFactor()).isEqualTo(2.6);
        assertThat(Sm2.initial().next(GOOD).easeFactor()).isEqualTo(2.5);
        assertThat(Sm2.initial().next(HARD).easeFactor()).isEqualTo(2.36);
    }

    @Test
    void easeFactorNeverDropsBelowMinimum() {
        Sm2 state = Sm2.initial();
        for (int i = 0; i < 20; i++) {
            state = state.next(AGAIN);
        }

        assertThat(state.easeFactor()).isEqualTo(Sm2.MIN_EASE);
    }

    @Test
    void dueDateIsTodayPlusInterval() {
        LocalDate today = LocalDate.of(2026, 1, 10);

        assertThat(new Sm2(2.5, 6, 2).dueDateFrom(today)).isEqualTo(LocalDate.of(2026, 1, 16));
    }
}
