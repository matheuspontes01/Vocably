package com.vocably.word;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordRepository extends JpaRepository<Word, Long> {

    // O Spring Data gera o SQL a partir do nome do método:
    // WHERE lower(term) LIKE lower('%' || :term || '%')
    Page<Word> findByTermContainingIgnoreCase(String term, Pageable pageable);
}
