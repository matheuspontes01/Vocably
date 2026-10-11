package com.vocably.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.beta.AnthropicBeta;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.anthropic.models.beta.messages.StructuredMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/** Implementação de {@link VocabularyAi} usando a API do Claude (SDK oficial da Anthropic). */
@Service
public class ClaudeVocabularyAi implements VocabularyAi {

    private static final Logger log = LoggerFactory.getLogger(ClaudeVocabularyAi.class);

    private static final String LOOKUP_SYSTEM_PROMPT = """
            You are a bilingual English–Brazilian Portuguese dictionary for a Brazilian student \
            who saves unknown words while reading books or watching videos. When a context \
            sentence is given, translate and define the word with the meaning it has in that \
            sentence, not just its most common meaning.""";

    private static final String QUIZ_SYSTEM_PROMPT = """
            You write vocabulary quizzes for a Brazilian student learning English. Write one \
            multiple-choice question per word you receive, alternating between FILL_IN_THE_BLANK \
            and MEANING_IN_CONTEXT. Use new, natural sentences (do not copy the student's \
            context sentence). Wrong options must be plausible but clearly wrong for a careful \
            reader; prefer other words from the list as distractors when they fit.""";

    private final AnthropicClient client;
    private final String model;

    public ClaudeVocabularyAi(AiProperties properties) {
        this.model = properties.model();
        // Sem chave configurada o app continua funcionando; só os recursos de IA ficam indisponíveis.
        this.client = properties.isConfigured()
                ? AnthropicOkHttpClient.builder()
                        .fromEnv() // lê ANTHROPIC_BASE_URL etc., se existirem
                        .apiKey(properties.apiKey())
                        .timeout(Duration.ofSeconds(90))
                        .build()
                : null;
    }

    @Override
    public WordLookup lookup(String term, String contextSentence) {
        String input = "Word: " + term
                + (contextSentence == null || contextSentence.isBlank() ? "" : "\nContext sentence: " + contextSentence);
        // Esforço baixo: é uma tarefa simples e queremos resposta rápida enquanto o usuário espera.
        return ask(LOOKUP_SYSTEM_PROMPT, input, WordLookup.class, BetaOutputConfig.Effort.LOW, 4_000);
    }

    @Override
    public List<GeneratedQuestion> generateQuiz(List<QuizWord> words) {
        String input = words.stream()
                .map(w -> "- id=%d | %s = %s%s".formatted(w.id(), w.term(), w.translation(),
                        w.contextSentence() == null ? "" : " | seen in: \"" + w.contextSentence() + "\""))
                .collect(Collectors.joining("\n", "Words:\n", ""));
        return ask(QUIZ_SYSTEM_PROMPT, input, GeneratedQuiz.class, BetaOutputConfig.Effort.MEDIUM, 16_000)
                .questions();
    }

    private <T> T ask(String system, String userMessage, Class<T> outputType, BetaOutputConfig.Effort effort, long maxTokens) {
        if (client == null) {
            throw new AiUnavailableException(
                    "AI features are disabled: set vocably.ai.api-key (or the ANTHROPIC_API_KEY environment variable)");
        }

        var params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .system(system)
                .addUserMessage(userMessage)
                // Saída estruturada: a resposta vem no formato do record outputType, já convertida.
                .outputConfig(outputType, effort)
                // Se o modelo principal recusar o pedido, a API tenta automaticamente outro modelo.
                .addBeta(AnthropicBeta.SERVER_SIDE_FALLBACK_2026_07_01)
                .fallbacksDefault()
                .build();

        try {
            StructuredMessage<T> response = client.beta().messages().create(params);

            BetaStopReason stopReason = response.stopReason().orElse(null);
            if (BetaStopReason.REFUSAL.equals(stopReason) || BetaStopReason.MAX_TOKENS.equals(stopReason)) {
                throw new AiUnavailableException("The AI could not answer this request (" + stopReason + ")");
            }

            return response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(text -> text.text())
                    .findFirst()
                    .orElseThrow(() -> new AiUnavailableException("The AI returned an empty answer"));
        } catch (AnthropicException e) {
            log.warn("Claude API call failed", e);
            throw new AiUnavailableException("The AI service is unavailable right now, try again in a moment", e);
        }
    }
}
