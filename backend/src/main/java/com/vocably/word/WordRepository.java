package com.vocably.word;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {

    // O Spring Data gera o SQL a partir do nome do método. Todas as consultas filtram por
    // userId: um usuário nunca enxerga as palavras de outro.
    Page<Word> findByUserId(Long userId, Pageable pageable);

    // WHERE user_id = ? AND lower(term) LIKE lower('%' || ? || '%')
    Page<Word> findByUserIdAndTermContainingIgnoreCase(Long userId, String term, Pageable pageable);

    Optional<Word> findByIdAndUserId(Long id, Long userId);

    Optional<Word> findFirstByUserIdAndTermIgnoreCase(Long userId, String term);

    // SQL "nativo" (do PostgreSQL) porque JPQL não tem ORDER BY random().
    @Query(value = "SELECT * FROM words WHERE user_id = :userId ORDER BY random() LIMIT :limit", nativeQuery = true)
    List<Word> findRandom(Long userId, int limit);
}
