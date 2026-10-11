CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Palavras criadas antes da autenticação não têm dono. Como o projeto ainda não tem
-- dados reais, removê-las é mais simples do que inventar um usuário para elas.
DELETE FROM words;

ALTER TABLE words
    ADD COLUMN user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE;

CREATE INDEX idx_words_user_id ON words (user_id);
