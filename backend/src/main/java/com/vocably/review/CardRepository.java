package com.vocably.review;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    // JOIN FETCH traz a palavra na mesma consulta. Sem ele, montar 20 cards dispararia
    // 20 SELECTs extras na tabela words (o famoso problema "N+1").
    @Query("""
            SELECT c FROM Card c JOIN FETCH c.word
            WHERE c.userId = :userId AND c.dueDate <= :today
            ORDER BY c.dueDate, c.id
            """)
    List<Card> findDue(Long userId, LocalDate today, Pageable pageable);

    long countByUserIdAndDueDateLessThanEqual(Long userId, LocalDate today);

    Optional<Card> findByIdAndUserId(Long id, Long userId);
}
