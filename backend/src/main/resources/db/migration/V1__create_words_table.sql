CREATE TABLE words (
    id               BIGSERIAL PRIMARY KEY,
    term             VARCHAR(100) NOT NULL,
    translation      VARCHAR(255),
    context_sentence TEXT,
    source_type      VARCHAR(20)  NOT NULL DEFAULT 'OTHER',
    source_title     VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
