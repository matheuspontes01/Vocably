import { useState } from "react";
import { compact, niceTicks, roundedTopBar } from "./scale";
import { Tooltip, type TooltipData } from "./Tooltip";
import { useWidth } from "./useWidth";

export interface ColumnDatum {
  key: string;
  label: string; // rótulo curto do eixo X (ex.: "11/10")
  tooltipLabel: string; // rótulo completo no tooltip (ex.: "sáb, 11 de out.")
  value: number;
}

const HEIGHT = 220;
const MARGIN = { top: 20, right: 8, bottom: 28, left: 36 };

/** Colunas verticais (uma série). Usado para "revisões por dia". */
export function ColumnChart({ data, unit, ariaLabel }: { data: ColumnDatum[]; unit: (n: number) => string; ariaLabel: string }) {
  const [ref, width] = useWidth<HTMLDivElement>();
  const [tooltip, setTooltip] = useState<TooltipData | null>(null);
  const [active, setActive] = useState<string | null>(null);

  const plotW = Math.max(0, width - MARGIN.left - MARGIN.right);
  const plotH = HEIGHT - MARGIN.top - MARGIN.bottom;
  const max = Math.max(0, ...data.map((d) => d.value));
  const ticks = niceTicks(max);
  const top = ticks[ticks.length - 1];
  const band = data.length ? plotW / data.length : 0;
  const barW = Math.max(2, Math.min(24, band - 2)); // no máximo 24px, sempre com um respiro entre colunas
  const y = (v: number) => MARGIN.top + plotH - (v / top) * plotH;
  const labelEvery = Math.ceil(data.length / Math.max(1, Math.floor(plotW / 48))); // não sobrepor rótulos
  const maxIndex = data.findIndex((d) => d.value === max && max > 0);

  function show(d: ColumnDatum, i: number) {
    setActive(d.key);
    setTooltip({ x: MARGIN.left + band * i + band / 2, y: y(d.value), value: unit(d.value), label: d.tooltipLabel });
  }

  return (
    <div className="chart" ref={ref} onPointerLeave={() => { setTooltip(null); setActive(null); }}>
      {width > 0 && (
        <svg width={width} height={HEIGHT} role="img" aria-label={ariaLabel}>
          {ticks.map((t) => (
            <g key={t}>
              <line className={t === 0 ? "chart-baseline" : "chart-grid"} x1={MARGIN.left} x2={width - MARGIN.right} y1={y(t)} y2={y(t)} />
              <text className="chart-axis" x={MARGIN.left - 6} y={y(t)} dy="0.32em" textAnchor="end">
                {compact(t)}
              </text>
            </g>
          ))}
          {data.map((d, i) => {
            const x = MARGIN.left + band * i + (band - barW) / 2;
            const h = y(0) - y(d.value);
            return (
              <g key={d.key}>
                {d.value > 0 && (
                  <path className={`chart-bar ${active === d.key ? "active" : ""}`} d={roundedTopBar(x, y(d.value), barW, h)} />
                )}
                {i === maxIndex && (
                  <text className="chart-value" x={x + barW / 2} y={y(d.value) - 6} textAnchor="middle">
                    {d.value}
                  </text>
                )}
                {i % labelEvery === (data.length - 1) % labelEvery && (
                  <text className="chart-axis" x={x + barW / 2} y={HEIGHT - 8} textAnchor="middle">
                    {d.label}
                  </text>
                )}
                {/* Área de toque: a faixa inteira do dia, bem maior que a coluna. */}
                <rect
                  className="chart-hit"
                  x={MARGIN.left + band * i}
                  y={MARGIN.top}
                  width={band}
                  height={plotH}
                  tabIndex={0}
                  aria-label={`${d.tooltipLabel}: ${unit(d.value)}`}
                  onPointerMove={() => show(d, i)}
                  onFocus={() => show(d, i)}
                  onBlur={() => { setTooltip(null); setActive(null); }}
                />
              </g>
            );
          })}
        </svg>
      )}
      <Tooltip data={tooltip} />
    </div>
  );
}
