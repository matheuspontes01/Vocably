package com.vocably.review;

import com.vocably.shared.ResourceNotFoundException;
import com.vocably.word.WordCreatedEvent;
import com.vocably.word.WordRepository;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    private final CardRepository cards;
    private final ReviewLogRepository logs;
    private final WordRepository words;
    // Clock injetado em vez de LocalDate.now(): nos testes dá para "viajar no tempo".
    private final Clock clock;

    public ReviewService(CardRepository cards, ReviewLogRepository logs, WordRepository words, Clock clock) {
        this.cards = cards;
        this.logs = logs;
        this.words = words;
        this.clock = clock;
    }

    /**
     * Roda quando uma palavra é criada (evento publicado pelo WordService), dentro da mesma
     * transação. Assim o módulo "word" não precisa conhecer cards nem revisão.
     */
    @EventListener
    @Transactional
    public void onWordCreated(WordCreatedEvent event) {
        var word = words.getReferenceById(event.wordId());
        LocalDate today = LocalDate.now(clock);
        cards.saveAll(List.of(
                new Card(word, CardDirection.EN_TO_PT, today),
                new Card(word, CardDirection.PT_TO_EN, today)));
    }

    public List<CardResponse> dueCards(Long userId, int limit) {
        return cards.findDue(userId, LocalDate.now(clock), PageRequest.ofSize(limit)).stream()
                .map(CardResponse::from)
                .toList();
    }

    @Transactional
    public ReviewResult review(Long userId, Long cardId, ReviewGrade grade) {
        Card card = cards.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Card " + cardId + " not found"));

        card.review(grade, LocalDate.now(clock), Instant.now(clock));
        logs.save(new ReviewLog(card.getId(), userId, grade, Instant.now(clock)));

        return new ReviewResult(card.getId(), card.getIntervalDays(), card.getDueDate());
    }

    public ReviewSummary summary(Long userId) {
        LocalDate today = LocalDate.now(clock);
        Instant startOfDay = today.atStartOfDay(clock.getZone()).toInstant();
        return new ReviewSummary(
                cards.countByUserIdAndDueDateLessThanEqual(userId, today),
                logs.countByUserIdAndReviewedAtGreaterThanEqual(userId, startOfDay));
    }
}
