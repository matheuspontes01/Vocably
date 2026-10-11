package com.vocably.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Registro imutável de uma resposta. Nunca é alterado, só inserido. */
@Entity
@Table(name = "review_logs")
public class ReviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReviewGrade grade;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    protected ReviewLog() {
    }

    public ReviewLog(Long cardId, Long userId, ReviewGrade grade, Instant reviewedAt) {
        this.cardId = cardId;
        this.userId = userId;
        this.grade = grade;
        this.reviewedAt = reviewedAt;
    }

    public ReviewGrade getGrade() {
        return grade;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}
