package com.vocably.review;

import com.vocably.word.Word;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Aqui usamos um relacionamento de verdade: o card precisa do termo e da tradução da
    // palavra para montar frente e verso. LAZY = só busca a palavra quando for usada.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id", nullable = false, updatable = false)
    private Word word;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private CardDirection direction;

    @Column(name = "ease_factor", nullable = false)
    private double easeFactor;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays;

    @Column(nullable = false)
    private int repetitions;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    protected Card() {
    }

    public Card(Word word, CardDirection direction, LocalDate today) {
        this.word = word;
        this.userId = word.getUserId();
        this.direction = direction;
        apply(Sm2.initial());
        this.dueDate = today;
    }

    public void review(ReviewGrade grade, LocalDate today, Instant now) {
        Sm2 next = schedule().next(grade);
        apply(next);
        this.dueDate = next.dueDateFrom(today);
        this.lastReviewedAt = now;
    }

    private Sm2 schedule() {
        return new Sm2(easeFactor, intervalDays, repetitions);
    }

    private void apply(Sm2 state) {
        this.easeFactor = state.easeFactor();
        this.intervalDays = state.intervalDays();
        this.repetitions = state.repetitions();
    }

    public String front() {
        return direction == CardDirection.EN_TO_PT ? word.getTerm() : word.getTranslation();
    }

    public String back() {
        return direction == CardDirection.EN_TO_PT ? word.getTranslation() : word.getTerm();
    }

    public Long getId() {
        return id;
    }

    public Word getWord() {
        return word;
    }

    public Long getUserId() {
        return userId;
    }

    public CardDirection getDirection() {
        return direction;
    }

    public int getIntervalDays() {
        return intervalDays;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public double getEaseFactor() {
        return easeFactor;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }
}
