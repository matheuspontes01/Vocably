package com.vocably.word;

import com.vocably.shared.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Teste unitário: sem Spring e sem banco. O repository é um mock do Mockito,
 * então testamos só a lógica do service — e o teste roda em milissegundos.
 */
@ExtendWith(MockitoExtension.class)
class WordServiceTest {

    static final Long USER_ID = 7L;

    @Mock
    WordRepository repository;

    @Mock
    ApplicationEventPublisher events;

    @InjectMocks
    WordService service;

    @Test
    void createTrimsTermAndDefaultsSourceTypeToOther() {
        when(repository.save(any(Word.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WordResponse response = service.create(USER_ID, new WordRequest("  serendipity ", "acaso feliz", null, null, null));

        assertThat(response.term()).isEqualTo("serendipity");
        assertThat(response.sourceType()).isEqualTo(SourceType.OTHER);
        // Outros módulos (revisão) são avisados para criar os cards.
        verify(events).publishEvent(any(WordCreatedEvent.class));
    }

    @Test
    void updateChangesExistingWord() {
        Word word = new Word(USER_ID, "ubiquitous", "onipresente", null, SourceType.OTHER, null);
        when(repository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(word));

        WordResponse response = service.update(USER_ID, 1L,
                new WordRequest("ubiquitous", "onipresente", "Phones are ubiquitous.", SourceType.BOOK, "1984"));

        assertThat(response.translation()).isEqualTo("onipresente");
        assertThat(response.sourceType()).isEqualTo(SourceType.BOOK);
        assertThat(response.sourceTitle()).isEqualTo("1984");
    }

    @Test
    void findByIdThrowsWhenWordDoesNotBelongToUser() {
        when(repository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(USER_ID, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deleteDoesNothingWhenWordDoesNotBelongToUser() {
        when(repository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(USER_ID, 99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any());
    }
}
