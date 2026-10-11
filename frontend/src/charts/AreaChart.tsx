import { useState, type PointerEvent } from "react";
import { compact, niceTicks } from "./scale";
import { Tooltip, type TooltipData } from "./Tooltip";
import { useWidth } from "./useWidth";

export interface PointDatum {
  key: string;
  label: string;
  tooltipLabel: string;
  value: number;
}

const HEIGHT = 220;
const MARGIN = { top: 20, right: 40, bottom: 28, left: 36 };

/** Linha com área suave (uma série) e crosshair. Usado para "total de palavras salvas". */
export function AreaChart({ data, unit, ariaLabel }: { data: PointDatum[]; unit: (n: number) => string; ariaLabel: string }) {
  const [ref, width] = useWidth<HTMLDivElement>();
  const [hover, setHover] = useState<number | null>(null);

  const plotW = Math.max(0, width - MARGIN.left - MARGIN.right);
  const plotH = HEIGHT - MARGIN.top - MARGIN.bottom;
  const ticks = niceTicks(Math.max(0, ...data.map((d) => d.value)));
  const top = ticks[ticks.length - 1];
  const x = (i: number) => MARGIN.left + (data.length > 1 ? (i / (data.length - 1)) * plotW : plotW / 2);
  const y = (v: number) => MARGIN.top + plotH - (v / top) * plotH;

  const line = data.map((d, i) => `${i === 0 ? "M" : "L"}${x(i)},${y(d.value)}`).join(" ");
  const area = data.length ? `${line} L${x(data.length - 1)},${y(0)} L${x(0)},${y(0)} Z` : "";
  const labelEvery = Math.ceil(data.length / Math.max(1, Math.floor(plotW / 48)));
  const last = data[data.length - 1];

  // O crosshair "gruda" no dia mais próximo do ponteiro: ninguém precisa mirar numa linha de 2px.
  function onMove(e: PointerEvent<SVGRectElement>) {
    const box = e.currentTarget.getBoundingClientRect();
    const ratio = (e.clientX - box.left) / box.width;
    setHover(Math.max(0, Math.min(data.length - 1, Math.round(ratio * (data.length - 1)))));
  }

  const tooltip: TooltipData | null =
    hover === null ? null : { x: x(hover), y: y(data[hover].value), value: unit(data[hover].value), label: data[hover].tooltipLabel };

  return (
    <div className="chart" ref={ref}>
      {width > 0 && data.length > 0 && (
        <svg width={width} height={HEIGHT} role="img" aria-label={ariaLabel}>
          {ticks.map((t) => (
            <g key={t}>
              <line className={t === 0 ? "chart-baseline" : "chart-grid"} x1={MARGIN.left} x2={width - MARGIN.right} y1={y(t)} y2={y(t)} />
              <text className="chart-axis" x={MARGIN.left - 6} y={y(t)} dy="0.32em" textAnchor="end">
                {compact(t)}
              </text>
            </g>
          ))}
          {data.map((d, i) =>
            i % labelEvery === (data.length - 1) % labelEvery ? (
              <text key={d.key} className="chart-axis" x={x(i)} y={HEIGHT - 8} textAnchor="middle">
                {d.label}
              </text>
            ) : null,
          )}
          <path className="chart-area" d={area} />
          <path className="chart-line" d={line} />
          {hover !== null && <line className="chart-crosshair" x1={x(hover)} x2={x(hover)} y1={MARGIN.top} y2={y(0)} />}
          {/* Ponto final + valor: o número que importa é o total de hoje. */}
          <circle className="chart-dot" cx={x(data.length - 1)} cy={y(last.value)} r={4} />
          <text className="chart-value" x={x(data.length - 1) + 8} y={y(last.value)} dy="0.32em">
            {last.value}
          </text>
          {hover !== null && <circle className="chart-dot" cx={x(hover)} cy={y(data[hover].value)} r={4} />}
          <rect
            className="chart-hit"
            x={MARGIN.left}
            y={MARGIN.top}
            width={plotW}
            height={plotH}
            tabIndex={0}
            aria-label={`${ariaLabel}. Hoje: ${unit(last.value)}`}
            onPointerMove={onMove}
            onPointerLeave={() => setHover(null)}
            onFocus={() => setHover(data.length - 1)}
            onBlur={() => setHover(null)}
            onKeyDown={(e) => {
              if (e.key === "ArrowLeft") setHover((h) => Math.max(0, (h ?? data.length - 1) - 1));
              if (e.key === "ArrowRight") setHover((h) => Math.min(data.length - 1, (h ?? 0) + 1));
            }}
          />
        </svg>
      )}
      <Tooltip data={tooltip} />
    </div>
  );
}
