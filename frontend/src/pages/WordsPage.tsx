import { useEffect, useState, type FormEvent } from "react";
import { api, ApiError, type SourceType, type Word } from "../api";

const SOURCE_LABELS: Record<SourceType, string> = { BOOK: "Livro", VIDEO: "Vídeo", OTHER: "Outro" };

const EMPTY_FORM = { term: "", translation: "", contextSentence: "", sourceType: "BOOK" as SourceType, sourceTitle: "" };

export function WordsPage() {
  const [words, setWords] = useState<Word[] | null>(null);
  const [search, setSearch] = useState("");
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  // Recarrega a lista quando a busca muda (com um pequeno atraso para não chamar a API a cada tecla).
  useEffect(() => {
    const timer = setTimeout(() => api.listWords(search).then((page) => setWords(page.content)), search ? 250 : 0);
    return () => clearTimeout(timer);
  }, [search]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSaving(true);
    try {
      const created = await api.createWord({
        ...form,
        contextSentence: form.contextSentence || undefined,
        sourceTitle: form.sourceTitle || undefined,
      });
      setWords((current) => [created, ...(current ?? [])]);
      // Mantém a fonte preenchida: normalmente você salva várias palavras do mesmo livro/vídeo.
      setForm({ ...EMPTY_FORM, sourceType: form.sourceType, sourceTitle: form.sourceTitle });
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

  const update = (field: keyof typeof form) => (e: { target: { value: string } }) =>
    setForm({ ...form, [field]: e.target.value });

  return (
    <div className="stack">
      <h1>Palavras</h1>

      <form className="panel word-form" onSubmit={handleSubmit}>
        <div className="row">
          <label>
            Palavra em inglês
            <input value={form.term} onChange={update("term")} required maxLength={100} placeholder="ephemeral" />
          </label>
          <label>
            Tradução
            <input value={form.translation} onChange={update("translation")} required maxLength={255} placeholder="efêmero" />
          </label>
        </div>
        <label>
          Frase onde você encontrou (opcional)
          <input value={form.contextSentence} onChange={update("contextSentence")} placeholder="Fame is ephemeral." />
        </label>
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
        {error && <p className="error">{error}</p>}
        <button className="primary" disabled={saving}>
          {saving ? "Salvando..." : "Salvar palavra"}
        </button>
      </form>

      <input className="search" placeholder="Buscar palavra..." value={search} onChange={(e) => setSearch(e.target.value)} />

      {words === null ? (
        <p className="muted center">Carregando...</p>
      ) : words.length === 0 ? (
        <p className="muted center">{search ? "Nenhuma palavra encontrada." : "Nenhuma palavra salva ainda."}</p>
      ) : (
        <ul className="word-list">
          {words.map((word) => (
            <li key={word.id} className="panel word-item">
              <div>
                <strong>{word.term}</strong> <span className="muted">— {word.translation}</span>
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
