import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, type Card, type ReviewGrade } from "../api";

const GRADES: { grade: ReviewGrade; label: string; key: string }[] = [
  { grade: "AGAIN", label: "Errei", key: "1" },
  { grade: "HARD", label: "Difícil", key: "2" },
  { grade: "GOOD", label: "Bom", key: "3" },
  { grade: "EASY", label: "Fácil", key: "4" },
];

export function ReviewPage() {
  const [queue, setQueue] = useState<Card[] | null>(null);
  const [revealed, setRevealed] = useState(false);
  const [reviewed, setReviewed] = useState(0);
  const [sending, setSending] = useState(false);

  useEffect(() => {
    api.dueCards().then(setQueue);
  }, []);

  const card = queue?.[0];

  const answer = useCallback(
    async (grade: ReviewGrade) => {
      if (!card || sending) return;
      setSending(true);
      try {
        await api.review(card.id, grade);
        setReviewed((n) => n + 1);
        setRevealed(false);
        // "Errei" devolve o card para o fim da fila: você o vê de novo ainda nesta sessão.
        setQueue((q) => (q ? [...q.slice(1), ...(grade === "AGAIN" ? [card] : [])] : q));
      } finally {
        setSending(false);
      }
    },
    [card, sending],
  );

  // Atalhos de teclado: espaço vira o card, 1–4 respondem.
  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      if (event.code === "Space") {
        event.preventDefault();
        setRevealed(true);
      } else if (revealed) {
        const option = GRADES.find((g) => g.key === event.key);
        if (option) answer(option.grade);
      }
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [revealed, answer]);

  if (queue === null) return <p className="muted center">Carregando...</p>;

  if (!card) {
    return (
      <div className="panel center stack">
        <h2>{reviewed > 0 ? "Sessão concluída! 🎉" : "Nada para revisar agora"}</h2>
        <p className="muted">
          {reviewed > 0
            ? `Você fez ${reviewed} ${reviewed === 1 ? "revisão" : "revisões"}. Volte amanhã para continuar.`
            : "Adicione palavras novas ou volte amanhã."}
        </p>
        <div className="row center">
          <Link to="/" className="button">
            Início
          </Link>
          <Link to="/words" className="button primary">
            Adicionar palavras
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="review stack">
      <p className="muted center">
        {queue.length} {queue.length === 1 ? "card restante" : "cards restantes"} · {reviewed} revisados
      </p>

      <div className={`flashcard ${revealed ? "flipped" : ""}`} onClick={() => setRevealed(true)}>
        <div className="face front">
          <span className="direction">{card.direction === "EN_TO_PT" ? "Inglês → Português" : "Português → Inglês"}</span>
          <span className="word">{card.front}</span>
          <span className="muted hint">clique ou aperte espaço para ver a resposta</span>
        </div>
        <div className="face back">
          <span className="muted">{card.front}</span>
          <span className="word">{card.back}</span>
          {card.contextSentence && <p className="context">“{card.contextSentence}”</p>}
        </div>
      </div>

      {revealed ? (
        <div className="grades">
          {GRADES.map(({ grade, label, key }) => (
            <button key={grade} className={`grade ${grade.toLowerCase()}`} onClick={() => answer(grade)} disabled={sending}>
              {label}
              <kbd>{key}</kbd>
            </button>
          ))}
        </div>
      ) : (
        <button className="primary big" onClick={() => setRevealed(true)}>
          Mostrar resposta
        </button>
      )}
    </div>
  );
}
