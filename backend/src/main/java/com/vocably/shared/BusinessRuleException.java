package com.vocably.shared;

/** O pedido é válido, mas uma regra de negócio impede atendê-lo agora. Vira HTTP 422. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
