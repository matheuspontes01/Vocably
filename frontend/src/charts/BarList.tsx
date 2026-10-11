import { useState } from "react";
import { roundedRightBar } from "./scale";
import { Tooltip, type TooltipData } from "./Tooltip";
import { useWidth } from "./useWidth";

export interface BarDatum {
  key: string;
  label: string;
  hint?: string; // explicação curta, mostrada no tooltip
  value: number;
}

const ROW = 36;
const BAR = 20;
const LABEL_W = 96;
const VALUE_W = 44;

/** Barras horizontais com rótulo à esquerda e valor na ponta. Usado para estágios e notas. */
export function BarList({ data, unit, ariaLabel }: { data: BarDatum[]; unit: (n: number) => string; ariaLabel: string }) {
  const [ref, width] = useWidth<HTMLDivElement>();
  const [tooltip, setTooltip] = useState<TooltipData | null>(null);
  const [active, setActive] = useState<string | null>(null);

  const height = data.length * ROW;
  const plotW = Math.max(0, width - LABEL_W - VALUE_W);
  const max = Math.max(1, ...data.map((d) => d.value));

  function show(d: BarDatum, i: number) {
    setActive(d.key);
    setTooltip({ x: LABEL_W + (d.value / max) * plotW, y: i * ROW + 4, value: unit(d.value), label: d.hint ?? d.label });
  }

  return (
    <div className="chart" ref={ref} onPointerLeave={() => { setTooltip(null); setActive(null); }}>
      {width > 0 && (
        <svg width={width} height={height} role="img" aria-label={ariaLabel}>
          <line className="chart-baseline" x1={LABEL_W} x2={LABEL_W} y1={0} y2={height} />
          {data.map((d, i) => {
            const w = (d.value / max) * plotW;
            const yBar = i * ROW + (ROW - BAR) / 2;
            return (
              <g key={d.key}>
                <text className="chart-label" x={LABEL_W - 10} y={yBar + BAR / 2} dy="0.32em" textAnchor="end">
                  {d.label}
                </text>
                {d.value > 0 && <path className={`chart-bar ${active === d.key ? "active" : ""}`} d={roundedRightBar(LABEL_W, yBar, w, BAR)} />}
                <text className="chart-value" x={LABEL_W + w + 8} y={yBar + BAR / 2} dy="0.32em">
                  {d.value}
                </text>
                <rect
                  className="chart-hit"
                  x={0}
                  y={i * ROW}
                  width={width}
                  height={ROW}
                  tabIndex={0}
                  aria-label={`${d.label}: ${unit(d.value)}`}
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
