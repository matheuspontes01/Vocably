package com.vocably.word;

import com.vocably.shared.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WordService {

    private final WordRepository repository;

    // Injeção pelo construtor: o Spring passa o repository, e no teste passamos um mock.
    public WordService(WordRepository repository) {
        this.repository = repository;
    }

    public Page<WordResponse> list(String search, Pageable pageable) {
        Page<Word> page = (search == null || search.isBlank())
                ? repository.findAll(pageable)
                : repository.findByTermContainingIgnoreCase(search.trim(), pageable);
        return page.map(WordResponse::from);
    }

    public WordResponse findById(Long id) {
        return WordResponse.from(getWord(id));
    }

    @Transactional
    public WordResponse create(WordRequest request) {
        Word word = new Word(request.term(), request.translation(), request.contextSentence(),
                request.sourceType(), request.sourceTitle());
        return WordResponse.from(repository.save(word));
    }

    @Transactional
    public WordResponse update(Long id, WordRequest request) {
        Word word = getWord(id);
        word.update(request.term(), request.translation(), request.contextSentence(),
                request.sourceType(), request.sourceTitle());
        // Sem save(): dentro da transação o JPA detecta a mudança e faz o UPDATE sozinho ("dirty checking").
        return WordResponse.from(word);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getWord(id));
    }

    private Word getWord(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Word " + id + " not found"));
    }
}
