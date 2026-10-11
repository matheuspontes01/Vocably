package com.vocably.word;

import com.vocably.shared.ResourceNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WordService {

    private final WordRepository repository;
    private final ApplicationEventPublisher events;

    // Injeção pelo construtor: o Spring passa as dependências, e no teste passamos mocks.
    public WordService(WordRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    public Page<WordResponse> list(Long userId, String search, Pageable pageable) {
        Page<Word> page = (search == null || search.isBlank())
                ? repository.findByUserId(userId, pageable)
                : repository.findByUserIdAndTermContainingIgnoreCase(userId, search.trim(), pageable);
        return page.map(WordResponse::from);
    }

    public WordResponse findById(Long userId, Long id) {
        return WordResponse.from(getWord(userId, id));
    }

    @Transactional
    public WordResponse create(Long userId, WordRequest request) {
        Word word = new Word(userId, request.term(), request.translation(), request.contextSentence(),
                request.sourceType(), request.sourceTitle());
        Word saved = repository.save(word);
        events.publishEvent(new WordCreatedEvent(saved.getId(), userId));
        return WordResponse.from(saved);
    }

    @Transactional
    public WordResponse update(Long userId, Long id, WordRequest request) {
        Word word = getWord(userId, id);
        word.update(request.term(), request.translation(), request.contextSentence(),
                request.sourceType(), request.sourceTitle());
        // Sem save(): dentro da transação o JPA detecta a mudança e faz o UPDATE sozinho ("dirty checking").
        return WordResponse.from(word);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        repository.delete(getWord(userId, id));
    }

    // Palavra de outro usuário responde 404 (e não 403) para não revelar que o id existe.
    private Word getWord(Long userId, Long id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Word " + id + " not found"));
    }
}
