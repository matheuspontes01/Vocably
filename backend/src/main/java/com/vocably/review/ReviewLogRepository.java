package com.vocably.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, Long> {

    long countByUserIdAndReviewedAtGreaterThanEqual(Long userId, Instant since);
}
