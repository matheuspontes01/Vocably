package com.vocably.stats;

import com.vocably.review.CardRepository;
import com.vocably.review.ReviewGrade;
import com.vocably.review.ReviewLog;
import com.vocably.review.ReviewLogRepository;
import com.vocably.word.Word;
import com.vocably.word.WordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class StatsService {

    /** Quantos dias de histórico olhamos para calcular a sequência (streak). */
    static final int STREAK_LOOKBACK_DAYS = 365;

    private final WordRepository words;
    private final CardRepository cards;
    private final ReviewLogRepository logs;
    private final Clock clock;

    public StatsService(WordRepository words, CardRepository cards, ReviewLogRepository logs, Clock clock) {
        this.words = words;
        this.cards = cards;
        this.logs = logs;
        this.clock = clock;
    }

    public StatsResponse stats(Long userId, int days) {
        ZoneId zone = clock.getZone();
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(days - 1L);
        Instant fromInstant = from.atStartOfDay(zone).toInstant();

        // Busca o histórico de revisões uma vez só e reaproveita para tudo.
        int lookback = Math.max(days, STREAK_LOOKBACK_DAYS);
        List<ReviewLog> history = logs.findByUserIdAndReviewedAtGreaterThanEqual(
                userId, today.minusDays(lookback).atStartOfDay(zone).toInstant());

        List<ReviewLog> inPeriod = history.stream().filter(l -> !l.getReviewedAt().isBefore(fromInstant)).toList();
        List<ReviewGrade> grades = inPeriod.stream().map(ReviewLog::getGrade).toList();
        List<LocalDate> reviewDays = inPeriod.stream().map(l -> LocalDate.ofInstant(l.getReviewedAt(), zone)).toList();

        Set<LocalDate> activeDays = new HashSet<>();
        history.forEach(l -> activeDays.add(LocalDate.ofInstant(l.getReviewedAt(), zone)));

        List<LocalDate> wordDays = words.findByUserIdAndCreatedAtGreaterThanEqual(userId, fromInstant).stream()
                .map(Word::getCreatedAt)
                .map(created -> LocalDate.ofInstant(created, zone))
                .toList();
        long wordsBefore = words.countByUserIdAndCreatedAtBefore(userId, fromInstant);

        return new StatsResponse(
                days,
                words.countByUserId(userId),
                StatsCalculator.streak(activeDays, today),
                inPeriod.size(),
                StatsCalculator.accuracy(grades),
                StatsCalculator.perDay(reviewDays, from, today),
                StatsCalculator.cumulative(StatsCalculator.perDay(wordDays, from, today), wordsBefore),
                StatsCalculator.countStages(cards.findScheduleByUserId(userId)),
                StatsCalculator.countGrades(grades));
    }
}
