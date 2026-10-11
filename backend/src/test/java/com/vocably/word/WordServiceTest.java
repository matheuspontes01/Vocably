package com.vocably.word;

import com.vocably.shared.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Mock
    WordRepository repository;

    @InjectMocks
    WordService service;

    @Test
    void createTrimsTermAndDefaultsSourceTypeToOther() {
        when(repository.save(any(Word.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WordResponse response = service.create(new WordRequest("  serendipity ", "acaso feliz", null, null, null));

        assertThat(response.term()).isEqualTo("serendipity");
        assertThat(response.sourceType()).isEqualTo(SourceType.OTHER);
    }

    @Test
    void updateChangesExistingWord() {
        Word word = new Word("ubiquitous", null, null, SourceType.OTHER, null);
        when(repository.findById(1L)).thenReturn(Optional.of(word));

        WordResponse response = service.update(1L,
                new WordRequest("ubiquitous", "onipresente", "Phones are ubiquitous.", SourceType.BOOK, "1984"));

        assertThat(response.translation()).isEqualTo("onipresente");
        assertThat(response.sourceType()).isEqualTo(SourceType.BOOK);
        assertThat(response.sourceTitle()).isEqualTo("1984");
    }

    @Test
    void findByIdThrowsWhenWordDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deleteDoesNothingWhenWordDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any());
    }
}
