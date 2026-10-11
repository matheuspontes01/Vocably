package com.vocably.ai;

/** A IA não está configurada (sem chave) ou não respondeu. Vira HTTP 503. */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
