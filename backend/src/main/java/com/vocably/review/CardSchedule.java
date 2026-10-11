package com.vocably.review;

import java.time.Instant;

/** Projeção com só o necessário para classificar um card por estágio de aprendizado. */
public record CardSchedule(int intervalDays, Instant lastReviewedAt) {
}
