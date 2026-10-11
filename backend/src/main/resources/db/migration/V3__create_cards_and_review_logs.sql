-- A partir de agora toda palavra precisa de tradução: ela é o verso do flashcard.
UPDATE words SET translation = '' WHERE translation IS NULL;
ALTER TABLE words ALTER COLUMN translation SET NOT NULL;

-- Cada palavra gera dois cards (inglês → português e português → inglês).
-- O estado do SM-2 fica no próprio card.
CREATE TABLE cards (
    id               BIGSERIAL PRIMARY KEY,
    word_id          BIGINT       NOT NULL REFERENCES words (id) ON DELETE CASCADE,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    direction        VARCHAR(20)  NOT NULL,
    ease_factor      DOUBLE PRECISION NOT NULL DEFAULT 2.5,
    interval_days    INTEGER      NOT NULL DEFAULT 0,
    repetitions      INTEGER      NOT NULL DEFAULT 0,
    due_date         DATE         NOT NULL,
    last_reviewed_at TIMESTAMPTZ,
    UNIQUE (word_id, direction)
);

-- A fila do dia busca por usuário + data de vencimento.
CREATE INDEX idx_cards_user_due ON cards (user_id, due_date);

-- Histórico de cada resposta: base para estatísticas e para a gamificação.
CREATE TABLE review_logs (
    id          BIGSERIAL PRIMARY KEY,
    card_id     BIGINT      NOT NULL REFERENCES cards (id) ON DELETE CASCADE,
    user_id     BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    grade       VARCHAR(10) NOT NULL,
    reviewed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_review_logs_user_time ON review_logs (user_id, reviewed_at);

-- Palavras que já existiam ganham seus cards, vencendo hoje.
INSERT INTO cards (word_id, user_id, direction, due_date)
SELECT id, user_id, d.direction, CURRENT_DATE
FROM words
CROSS JOIN (VALUES ('EN_TO_PT'), ('PT_TO_EN')) AS d (direction);
