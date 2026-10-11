import { useEffect, useRef, useState, type FormEvent } from "react";
import { api, ApiError, type Definition, type SourceType, type Word, type WordLookup } from "../api";
import { SpeakButton } from "../SpeakButton";

const SOURCE_LABELS: Record<SourceType, string> = { BOOK: "Livro", VIDEO: "Vídeo", OTHER: "Outro" };

const EMPTY_FORM = {
  term: "",
  translation: "",
  contextSentence: "",
  sourceType: "BOOK" as SourceType,
  sourceTitle: "",
  partOfSpeech: "",
  phonetic: "",
  audioUrl: "",
  definition: "",
  exampleSentence: "",
};

type Form = typeof EMPTY_FORM;

/** Quebra a frase em palavras clicáveis, mantendo apóstrofos e hífens ("don't", "well-known"). */
function tokenize(sentence: string) {
  return sentence.match(/[A-Za-z]+(?:['’-][A-Za-z]+)*/g) ?? [];
}

export function WordsPage() {
  const [words, setWords] = useState<Word[] | null>(null);
  const [search, setSearch] = useState("");
  const [form, setForm] = useState<Form>(EMPTY_FORM);
  const [suggestion, setSuggestion] = useState<WordLookup | null>(null);
  const [existingWordId, setExistingWordId] = useState<number | null>(null);
  const [lookingUp, setLookingUp] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [savedMessage, setSavedMessage] = useState<string | null>(null);
  const termInput = useRef<HTMLInputElement>(null);

  // Recarrega a lista quando a busca muda (com um pequeno atraso para não chamar a API a cada tecla).
  useEffect(() => {
    const timer = setTimeout(() => api.listWords(search).then((page) => setWords(page.content)), search ? 250 : 0);
    return () => clearTimeout(timer);
  }, [search]);

  async function lookup(term: string) {
    if (!term.trim()) return;
    setError(null);
    setSavedMessage(null);
    setLookingUp(true);
    try {
      const result = await api.lookupWord(term.trim(), form.contextSentence.trim() || undefined);
      const s = result.suggestion;
      const first = s.definitions[0];
      setSuggestion(s);
      setExistingWordId(result.existingWordId);
      setForm((f) => ({
        ...f,
        term: s.term,
        translation: s.translation ?? "",
        phonetic: s.phonetic ?? "",
        audioUrl: s.audioUrl ?? "",
        partOfSpeech: first?.partOfSpeech ?? "",
        definition: first?.definition ?? "",
        exampleSentence: first?.example ?? "",
      }));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Não foi possível pesquisar a palavra.");
      setSuggestion(null);
      setForm((f) => ({ ...f, term: term.trim() }));
    } finally {
      setLookingUp(false);
    }
  }

  function chooseDefinition(d: Definition) {
    setForm({ ...form, partOfSpeech: d.partOfSpeech ?? "", definition: d.definition, exampleSentence: d.example ?? "" });
  }

  async function handleSave(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSaving(true);
    try {
      const created = await api.createWord({
        term: form.term,
        translation: form.translation,
        contextSentence: form.contextSentence || undefined,
        sourceType: form.sourceType,
        sourceTitle: form.sourceTitle || undefined,
        partOfSpeech: form.partOfSpeech || undefined,
        phonetic: form.phonetic || undefined,
        audioUrl: form.audioUrl || undefined,
        definition: form.definition || undefined,
        exampleSentence: form.exampleSentence || undefined,
      });
      setWords((current) => [created, ...(current ?? [])]);
      setSavedMessage(`"${created.term}" salva! 2 flashcards criados.`);
      // Mantém frase e fonte: é comum salvar várias palavras da mesma frase/livro.
      setForm({ ...EMPTY_FORM, contextSentence: form.contextSentence, sourceType: form.sourceType, sourceTitle: form.sourceTitle });
      setSuggestion(null);
      setExistingWordId(null);
      termInput.current?.focus();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Erro ao salvar.");
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(word: Word) {
    if (!confirm(`Remover "${word.term}" e seus flashcards?`)) return;
    await api.deleteWord(word.id);
    setWords((current) => current?.filter((w) => w.id !== word.id) ?? null);
  }

  const update = (field: keyof Form) => (e: { target: { value: string } }) =>
    setForm({ ...form, [field]: e.target.value });

  const tokens = tokenize(form.contextSentence);
  const hasTerm = form.term.trim().length > 0;
  const nothingFound = suggestion && !suggestion.translation && suggestion.definitions.length === 0;

  return (
    <div className="stack">
      <h1>Palavras</h1>

      <div className="panel word-form">
        <label>
          Frase onde você encontrou (opcional)
          <textarea
            rows={2}
            value={form.contextSentence}
            onChange={update("contextSentence")}
            placeholder="Cole aqui a frase do livro ou da legenda e clique nas palavras que não conhece"
          />
        </label>
        {tokens.length > 0 && (
          <div className="tokens" aria-label="Palavras da frase">
            {tokens.map((token, i) => (
              <button
                key={`${token}-${i}`}
                type="button"
                className={`token ${token.toLowerCase() === form.term.toLowerCase() ? "selected" : ""}`}
                onClick={() => lookup(token)}
                disabled={lookingUp}
              >
                {token}
              </button>
            ))}
          </div>
        )}

        <form
          className="lookup-row"
          onSubmit={(e) => {
            e.preventDefault();
            lookup(form.term);
          }}
        >
          <label>
            Palavra em inglês
            <input ref={termInput} value={form.term} onChange={update("term")} required maxLength={100} placeholder="ephemeral" />
          </label>
          <button className="primary" disabled={lookingUp || !hasTerm}>
            {lookingUp ? "Pesquisando..." : "Pesquisar"}
          </button>
        </form>

        {savedMessage && <p className="success">{savedMessage}</p>}
        {error && <p className="error">{error}</p>}

        {(suggestion || error) && hasTerm && (
          <form className="suggestion" onSubmit={handleSave}>
            {existingWordId && <p className="warning">Você já salvou esta palavra antes. Salvar de novo cria cards duplicados.</p>}
            {nothingFound && (
              <p className="warning">Não encontramos essa palavra no dicionário. Você pode preencher a tradução manualmente.</p>
            )}

            <div className="suggestion-header">
              <strong className="suggestion-term">{form.term}</strong>
              <SpeakButton text={form.term} audioUrl={form.audioUrl} />
              {form.phonetic && <span className="muted">{form.phonetic}</span>}
            </div>

            {suggestion?.contextTranslation && (
              <p className="note">
                <span className="muted">Tradução da frase:</span> {suggestion.contextTranslation}
              </p>
            )}

            <label>
              Tradução
              <input value={form.translation} onChange={update("translation")} required maxLength={255} />
            </label>
            {suggestion && suggestion.alternativeTranslations.length > 0 && (
              <div className="alternatives">
                <span className="muted">Outras traduções:</span>
                {suggestion.alternativeTranslations.map((alt) => (
                  <button key={alt} type="button" className="chip" onClick={() => setForm({ ...form, translation: alt })}>
                    {alt}
                  </button>
                ))}
              </div>
            )}

            {suggestion && suggestion.definitions.length > 0 && (
              <fieldset className="definitions">
                <legend>Significado (escolha o que combina com a frase)</legend>
                {suggestion.definitions.map((d) => (
                  <label key={d.definition} className={`definition-option ${form.definition === d.definition ? "selected" : ""}`}>
                    <input
                      type="radio"
                      name="definition"
                      checked={form.definition === d.definition}
                      onChange={() => chooseDefinition(d)}
                    />
                    <span>
                      {d.partOfSpeech && <span className="tag">{d.partOfSpeech}</span>} {d.definition}
                      {d.example && <em className="muted"> — “{d.example}”</em>}
                    </span>
                  </label>
                ))}
              </fieldset>
            )}

            <div className="row">
              <label>
                Fonte
                <select value={form.sourceType} onChange={update("sourceType")}>
                  {Object.entries(SOURCE_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Título (opcional)
                <input value={form.sourceTitle} onChange={update("sourceTitle")} maxLength={255} placeholder="Harry Potter" />
              </label>
            </div>

            <button className="primary" disabled={saving}>
              {saving ? "Salvando..." : "Salvar palavra"}
            </button>
          </form>
        )}
      </div>

      <input className="search" placeholder="Buscar nas suas palavras..." value={search} onChange={(e) => setSearch(e.target.value)} />

      {words === null ? (
        <p className="muted center">Carregando...</p>
      ) : words.length === 0 ? (
        <p className="muted center">{search ? "Nenhuma palavra encontrada." : "Nenhuma palavra salva ainda."}</p>
      ) : (
        <ul className="word-list">
          {words.map((word) => (
            <li key={word.id} className="panel word-item">
              <div>
                <strong>{word.term}</strong> <SpeakButton text={word.term} audioUrl={word.audioUrl} />{" "}
                <span className="muted">
                  {word.phonetic && `${word.phonetic} · `}
                  {word.translation}
                </span>
                {word.contextSentence && <p className="context">“{word.contextSentence}”</p>}
                <span className="tag">
                  {SOURCE_LABELS[word.sourceType]}
                  {word.sourceTitle && `: ${word.sourceTitle}`}
                </span>
              </div>
              <button className="link danger" onClick={() => handleDelete(word)}>
                Remover
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
