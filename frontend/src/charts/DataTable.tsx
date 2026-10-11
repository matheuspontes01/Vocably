/** Os mesmos números do gráfico em tabela: acessível e sem depender de passar o mouse. */
export function DataTable({ columns, rows }: { columns: [string, string]; rows: [string, string | number][] }) {
  return (
    <details className="chart-table">
      <summary>Ver dados em tabela</summary>
      <table>
        <thead>
          <tr>
            <th>{columns[0]}</th>
            <th>{columns[1]}</th>
          </tr>
        </thead>
        <tbody>
          {rows.map(([label, value]) => (
            <tr key={label}>
              <td>{label}</td>
              <td>{value}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </details>
  );
}
