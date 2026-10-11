import { useEffect, useState } from "react";
import { api, type CardStage, type ReviewGrade, type Stats } from "../api";
import { AreaChart } from "../charts/AreaChart";
import { BarList } from "../charts/BarList";
import { ColumnChart } from "../charts/ColumnChart";
import { DataTable } from "../charts/DataTable";

const PERIODS = [7, 30, 90];

const STAGES: Record<CardStage, { label: string; hint: string }> = {
  NEW: { label: "Novos", hint: "Ainda não revisados" },
  LEARNING: { label: "Aprendendo", hint: "Voltam em menos de 7 dias" },
  YOUNG: { label: "Recentes", hint: "Intervalo de 7 a 20 dias" },
  MATURE: { label: "Fixados", hint: "Intervalo de 21 dias ou mais" },
};

const GRADES: Record<ReviewGrade, string> = { AGAIN: "Errei", HARD: "Difícil", GOOD: "Bom", EASY: "Fácil" };

const plural = (n: number, one: string, many: string) => `${n} ${n === 1 ? one : many}`;
const reviews = (n: number) => plural(n, "revisão", "revisões");
const wordsUnit = (n: number) => plural(n, "palavra", "palavras");
const cardsUnit = (n: number) => plural(n, "card", "cards");
const answers = (n: number) => plural(n, "resposta", "respostas");

// As datas chegam como "2026-10-11"; o "T12:00" evita que o fuso horário mude o dia.
const toDate = (iso: string) => new Date(`${iso}T12:00:00`);
const shortDate = (iso: string) => toDate(iso).toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit" });
const longDate = (iso: string) => toDate(iso).toLocaleDateString("pt-BR", { weekday: "short", day: "numeric", month: "short" });

export function StatsPage() {
  const [days, setDays] = useState(30);
  const [stats, setStats] = useState<Stats | null>(null);
  const [loading, setLoading] = useState(true);

  // Ao trocar o período, mantém os gráficos antigos (esmaecidos) até chegar o novo resultado.
  useEffect(() => {
    setLoading(true);
    api
      .stats(days)
      .then(setStats)
      .finally(() => setLoading(false));
  }, [days]);

  return (
    <div className="stack">
      <h1>Progresso</h1>

      <div className="segmented" role="group" aria-label="Período">
        {PERIODS.map((p) => (
          <button key={p} className={p === days ? "selected" : ""} aria-pressed={p === days} onClick={() => setDays(p)}>
            {p} dias
          </button>
        ))}
      </div>

      {!stats ? (
        <p className="muted center">Carregando...</p>
      ) : (
        <div className={`stack ${loading ? "refreshing" : ""}`}>
          <div className="stats">
            <Tile label="Sequência" value={plural(stats.currentStreak, "dia", "dias")} />
            <Tile label="Palavras salvas" value={stats.totalWords} />
            <Tile label={`Revisões em ${stats.days} dias`} value={stats.reviewsInPeriod} />
            <Tile label="Taxa de acerto" value={stats.accuracy === null ? "–" : `${Math.round(stats.accuracy * 100)}%`} />
          </div>

          <ChartCard title="Revisões por dia" subtitle={`Últimos ${stats.days} dias`}>
            {stats.reviewsInPeriod === 0 ? (
              <Empty text="Nenhuma revisão neste período ainda." />
            ) : (
              <ColumnChart
                ariaLabel="Revisões por dia"
                unit={reviews}
                data={stats.reviewsPerDay.map((d) => ({ key: d.date, label: shortDate(d.date), tooltipLabel: longDate(d.date), value: d.count }))}
              />
            )}
            <DataTable columns={["Dia", "Revisões"]} rows={stats.reviewsPerDay.map((d) => [longDate(d.date), d.count])} />
          </ChartCard>

          <ChartCard title="Palavras salvas" subtitle="Total acumulado">
            {stats.totalWords === 0 ? (
              <Empty text="Salve sua primeira palavra para ver a evolução." />
            ) : (
              <AreaChart
                ariaLabel="Total de palavras salvas por dia"
                unit={wordsUnit}
                data={stats.totalWordsPerDay.map((d) => ({ key: d.date, label: shortDate(d.date), tooltipLabel: longDate(d.date), value: d.count }))}
              />
            )}
            <DataTable columns={["Dia", "Total de palavras"]} rows={stats.totalWordsPerDay.map((d) => [longDate(d.date), d.count])} />
          </ChartCard>

          <div className="chart-grid-2">
            <ChartCard title="Estágio dos cards" subtitle="Quanto maior o intervalo, mais fixada a palavra">
              <BarList
                ariaLabel="Cards por estágio"
                unit={cardsUnit}
                data={(Object.keys(STAGES) as CardStage[]).map((s) => ({
                  key: s,
                  label: STAGES[s].label,
                  hint: STAGES[s].hint,
                  value: stats.cardStages[s],
                }))}
              />
              <DataTable
                columns={["Estágio", "Cards"]}
                rows={(Object.keys(STAGES) as CardStage[]).map((s) => [`${STAGES[s].label} (${STAGES[s].hint.toLowerCase()})`, stats.cardStages[s]])}
              />
            </ChartCard>

            <ChartCard title="Respostas por nota" subtitle={`Últimos ${stats.days} dias`}>
              <BarList
                ariaLabel="Respostas por nota"
                unit={answers}
                data={(Object.keys(GRADES) as ReviewGrade[]).map((g) => ({ key: g, label: GRADES[g], value: stats.grades[g] }))}
              />
              <DataTable columns={["Nota", "Respostas"]} rows={(Object.keys(GRADES) as ReviewGrade[]).map((g) => [GRADES[g], stats.grades[g]])} />
            </ChartCard>
          </div>
        </div>
      )}
    </div>
  );
}

function Tile({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="panel stat">
      <span className="stat-value">{value}</span>
      <span className="muted">{label}</span>
    </div>
  );
}

function ChartCard({ title, subtitle, children }: { title: string; subtitle: string; children: React.ReactNode }) {
  return (
    <section className="panel chart-card">
      <header>
        <h2>{title}</h2>
        <p className="muted">{subtitle}</p>
      </header>
      {children}
    </section>
  );
}

function Empty({ text }: { text: string }) {
  return <p className="muted center chart-empty">{text}</p>;
}
