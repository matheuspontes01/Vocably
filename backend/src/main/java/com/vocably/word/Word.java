package com.vocably.word;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "words")
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guardamos só o id do dono (e não um @ManyToOne User) para o módulo "word"
    // não depender das classes do módulo "auth".
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String term;

    @Column(nullable = false)
    private String translation;

    @Column(name = "context_sentence")
    private String contextSentence;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType = SourceType.OTHER;

    @Column(name = "source_title")
    private String sourceTitle;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Exigido pelo JPA; não use no código da aplicação. */
    protected Word() {
    }

    public Word(Long userId, String term, String translation, String contextSentence, SourceType sourceType, String sourceTitle) {
        this.userId = userId;
        update(term, translation, contextSentence, sourceType, sourceTitle);
    }

    /** Em vez de vários setters, um único método mantém a entidade sempre consistente. */
    public void update(String term, String translation, String contextSentence, SourceType sourceType, String sourceTitle) {
        this.term = term.trim();
        this.translation = translation.trim();
        this.contextSentence = contextSentence;
        this.sourceType = sourceType != null ? sourceType : SourceType.OTHER;
        this.sourceTitle = sourceTitle;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTerm() {
        return term;
    }

    public String getTranslation() {
        return translation;
    }

    public String getContextSentence() {
        return contextSentence;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
