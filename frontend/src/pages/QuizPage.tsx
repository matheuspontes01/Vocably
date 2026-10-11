import { useState } from "react";
import { Link } from "react-router-dom";
import { api, ApiError, type QuizQuestion } from "../api";
import { SpeakButton } from "../SpeakButton";

const LETTERS = ["A", "B", "C", "D"];

const TYPE_LABELS: Record<QuizQuestion["type"], string> = {
  FILL_IN_THE_BLANK: "Complete a frase",
  MEANING: "Significado",
  REVERSE: "Em inglês",
};

export function QuizPage() {
  const [questions, setQuestions] = useState<QuizQuestion[] | null>(null);
  const [index, setIndex] = useState(0);
  const [chosen, setChosen] = useState<number | null>(null);
  const [score, setScore] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function start() {
    setLoading(true);
    setError(null);
    try {
      setQuestions(await api.generateQuiz(10));
      setIndex(0);
      setChosen(null);
      setScore(0);
    } catch (e) {
      setError(
        e instanceof ApiError && e.status === 422
          ? "Salve pelo menos 4 palavras para fazer o quiz: as alternativas erradas vêm das suas outras palavras."
          : "Não foi possível gerar o quiz.",
      );
    } finally {
      setLoading(false);
    }
  }

  function choose(option: number) {
    if (chosen !== null || !questions) return;
    setChosen(option);
    if (option === questions[index].correctOption) setScore((s) => s + 1);
  }

  if (questions === null || loading) {
    return (
      <div className="panel center stack">
        <h2>Quiz</h2>
        <p className="muted">10 perguntas com as suas palavras: complete a frase, escolha o significado ou a palavra em inglês.</p>
        {error && <p className="error">{error}</p>}
        <div>
          <button className="primary big" onClick={start} disabled={loading}>
            {loading ? "Gerando perguntas..." : "Começar quiz"}
          </button>
        </div>
      </div>
    );
  }

  if (questions.length === 0) {
    return (
      <div className="panel center stack">
        <h2>Nenhuma palavra ainda</h2>
        <p className="muted">Salve algumas palavras para poder fazer o quiz.</p>
        <div>
          <Link to="/words" className="button primary">
            Adicionar palavras
          </Link>
        </div>
      </div>
    );
  }

  if (index >= questions.length) {
    return (
      <div className="panel center stack">
        <h2>
          {score} de {questions.length} {score === questions.length ? "🏆" : score >= questions.length / 2 ? "👏" : "💪"}
        </h2>
        <p className="muted">{score === questions.length ? "Perfeito!" : "Errar faz parte: as palavras vão aparecer de novo."}</p>
        <div className="row center">
          <Link to="/" className="button">
            Início
          </Link>
          <button className="primary" onClick={start}>
            Novo quiz
          </button>
        </div>
      </div>
    );
  }

  const question = questions[index];
  const answered = chosen !== null;

  return (
    <div className="stack">
      <p className="muted center">
        Pergunta {index + 1} de {questions.length} · {score} {score === 1 ? "acerto" : "acertos"}
      </p>

      <div className="panel quiz-question">
        <span className="direction">{TYPE_LABELS[question.type]}</span>
        <p className="prompt">{question.prompt}</p>
        {question.sentence && <p className="quiz-sentence">{question.sentence}</p>}
      </div>

      <div className="options">
        {question.options.map((option, i) => {
          const state = !answered ? "" : i === question.correctOption ? "correct" : i === chosen ? "wrong" : "dim";
          return (
            <button key={option} className={`option ${state}`} onClick={() => choose(i)} disabled={answered}>
              <span className="letter">{LETTERS[i]}</span> {option}
            </button>
          );
        })}
      </div>

      {answered && (
        <div className={`panel feedback ${chosen === question.correctOption ? "correct" : "wrong"}`}>
          <strong>{chosen === question.correctOption ? "Acertou!" : "Não foi dessa vez."}</strong>{" "}
          <span>
            {question.term} <SpeakButton text={question.term} />
          </span>
          <p>{question.explanation}</p>
          <button
            className="primary"
            onClick={() => {
              setIndex(index + 1);
              setChosen(null);
            }}
          >
            {index + 1 < questions.length ? "Próxima" : "Ver resultado"}
          </button>
        </div>
      )}
    </div>
  );
}
