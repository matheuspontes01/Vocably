package com.vocably.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaudeVocabularyAiTest {

    @Test
    void failsWithClearMessageWhenApiKeyIsMissing() {
        var ai = new ClaudeVocabularyAi(new AiProperties("", "claude-opus-5-5"));

        assertThatThrownBy(() -> ai.lookup("bank", null))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("api-key");
    }
}
