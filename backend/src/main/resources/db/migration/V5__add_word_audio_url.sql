-- Os detalhes da palavra (V4) agora vêm de APIs gratuitas: Free Dictionary (pronúncia,
-- definição, exemplo) e MyMemory (tradução). O áudio da pronúncia é um link do Free Dictionary.
ALTER TABLE words ADD COLUMN IF NOT EXISTS audio_url VARCHAR(500);

-- As APIs gratuitas não traduzem frases de exemplo; a coluna não é mais usada.
ALTER TABLE words DROP COLUMN IF EXISTS example_translation;
