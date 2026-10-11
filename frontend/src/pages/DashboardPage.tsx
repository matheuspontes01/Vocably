import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api, type ReviewSummary } from "../api";
import { useAuth } from "../auth";

export function DashboardPage() {
  const { session } = useAuth();
  const [summary, setSummary] = useState<ReviewSummary | null>(null);
  const [totalWords, setTotalWords] = useState<number | null>(null);

  useEffect(() => {
    api.summary().then(setSummary);
    api.listWords().then((page) => setTotalWords(page.page.totalElements));
  }, []);

  return (
    <div className="stack">
      <h1>Olá, {session?.displayName}!</h1>

      <div className="stats">
        <Stat label="Cards para hoje" value={summary?.dueToday} />
        <Stat label="Revisados hoje" value={summary?.reviewedToday} />
        <Stat label="Palavras salvas" value={totalWords} />
      </div>
      <Link to="/progress" className="link-subtle">
        Ver gráficos de progresso →
      </Link>

      {summary && summary.dueToday > 0 ? (
        <div className="row actions">
          <Link to="/review" className="button primary big">
            Revisar {summary.dueToday} {summary.dueToday === 1 ? "card" : "cards"}
          </Link>
          <Link to="/quiz" className="button big">
            Fazer um quiz
          </Link>
        </div>
      ) : (
        <div className="panel center">
          <p>{totalWords === 0 ? "Comece salvando as palavras que você encontrar." : "Tudo revisado por hoje! 🎉"}</p>
          <div className="row center">
            <Link to="/words" className="button">
              Adicionar palavras
            </Link>
            {!!totalWords && (
              <Link to="/quiz" className="button primary">
                Fazer um quiz
              </Link>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: number | null | undefined }) {
  return (
    <div className="panel stat">
      <span className="stat-value">{value ?? "–"}</span>
      <span className="muted">{label}</span>
    </div>
  );
}
