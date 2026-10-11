/** Um máximo "redondo" para o eixo Y (ex.: 37 → 40) e os ticks correspondentes. */
export function niceTicks(max: number, count = 4): number[] {
  if (max <= 0) return [0, 1];
  const rawStep = max / count;
  const magnitude = 10 ** Math.floor(Math.log10(rawStep));
  const step = [1, 2, 5, 10].map((m) => m * magnitude).find((s) => s >= rawStep) ?? rawStep;
  // Continua até o último tick cobrir o máximo: nenhuma barra pode passar do topo do eixo.
  const ticks = [0];
  while (ticks[ticks.length - 1] < max) {
    ticks.push(Math.round((ticks[ticks.length - 1] + step) * 1000) / 1000);
  }
  return ticks;
}

/** 1234 → "1,2 mil": rótulos curtos no eixo. */
export function compact(value: number): string {
  return new Intl.NumberFormat("pt-BR", { notation: "compact", maximumFractionDigits: 1 }).format(value);
}

/** Retângulo com só o topo arredondado (a base fica reta, encostada na linha de base). */
export function roundedTopBar(x: number, y: number, width: number, height: number, radius = 4): string {
  const r = Math.min(radius, width / 2, height);
  return `M${x},${y + height} V${y + r} Q${x},${y} ${x + r},${y} H${x + width - r} Q${x + width},${y} ${x + width},${y + r} V${y + height} Z`;
}

/** Retângulo com só a ponta direita arredondada (para barras horizontais). */
export function roundedRightBar(x: number, y: number, width: number, height: number, radius = 4): string {
  const r = Math.min(radius, height / 2, width);
  return `M${x},${y} H${x + width - r} Q${x + width},${y} ${x + width},${y + r} V${y + height - r} Q${x + width},${y + height} ${x + width - r},${y + height} H${x} Z`;
}
