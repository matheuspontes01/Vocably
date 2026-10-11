package com.vocably.auth;

import java.time.Instant;

/** Resposta de cadastro e login: o token que o cliente deve mandar no header Authorization. */
public record AuthResponse(String token, Instant expiresAt, Long userId, String displayName) {
}
