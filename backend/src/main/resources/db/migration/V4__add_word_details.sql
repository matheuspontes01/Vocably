-- Detalhes preenchidos pela pesquisa com IA (todos opcionais).
ALTER TABLE words
    ADD COLUMN part_of_speech      VARCHAR(50),
    ADD COLUMN phonetic            VARCHAR(100),
    ADD COLUMN definition          TEXT,
    ADD COLUMN example_sentence    TEXT,
    ADD COLUMN example_translation TEXT;

-- Busca de palavra repetida ("você já salvou esta palavra").
CREATE INDEX idx_words_user_term ON words (user_id, lower(term));
