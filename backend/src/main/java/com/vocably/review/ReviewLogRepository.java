package com.vocably.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, Long> {

    long countByUserIdAndReviewedAtGreaterThanEqual(Long userId, Instant since);

    List<ReviewLog> findByUserIdAndReviewedAtGreaterThanEqual(Long userId, Instant since);
}
