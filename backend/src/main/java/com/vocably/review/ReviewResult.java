package com.vocably.review;

import java.time.LocalDate;

/** Quando o card vai aparecer de novo, depois da resposta. */
public record ReviewResult(Long cardId, int intervalDays, LocalDate nextDueDate) {
}
