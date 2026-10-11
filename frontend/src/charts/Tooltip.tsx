export interface TooltipData {
  x: number;
  y: number;
  value: string;
  label: string;
}

/** Valor em destaque, rótulo secundário (o leitor já sabe a série; quer o número). */
export function Tooltip({ data }: { data: TooltipData | null }) {
  if (!data) return null;
  return (
    <div className="chart-tooltip" style={{ left: data.x, top: data.y }} role="status">
      <strong>{data.value}</strong>
      <span>{data.label}</span>
    </div>
  );
}
