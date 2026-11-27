export type Estimativa = {
  valorEstimado: null;
  piso: null;
  limiarAlerta: null;
  motivo: string;
} | {
  distanciaKm: number;
  consumoKmLitro: number;
  precoLitro: number;
  fonteCombustivel: string;
  dataCombustivel: string;
  combustivel: number;
  custosOperacionais: number;
  remuneracao: number;
  valorEstimado: number;
  piso: number | null;
  limiarAlerta: number | null;
  motivo: null;
};

const moeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const decimal = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 2 });

export function EstimativaFrete({ estimativa }: { estimativa: Estimativa }) {
  if (estimativa.valorEstimado == null) return <p>Valor estimado indisponível. {estimativa.motivo}</p>;

  return <details>
    <summary>Valor estimado do frete · {moeda.format(estimativa.valorEstimado)}</summary>
    <dl className="mt-2 grid gap-1 text-sm">
      <div><dt className="inline font-semibold">Distância viária </dt><dd className="inline">{decimal.format(estimativa.distanciaKm)} km</dd></div>
      <div><dt className="inline font-semibold">Consumo de referência </dt><dd className="inline">{decimal.format(estimativa.consumoKmLitro)} km/L</dd></div>
      <div><dt className="inline font-semibold">Combustível </dt><dd className="inline">{moeda.format(estimativa.combustivel)} a {moeda.format(estimativa.precoLitro)}/L</dd></div>
      <div><dt className="inline font-semibold">Fonte do preço </dt><dd className="inline">{estimativa.fonteCombustivel}, {estimativa.dataCombustivel.split("-").reverse().join("/")}</dd></div>
      <div><dt className="inline font-semibold">Outros custos operacionais </dt><dd className="inline">{moeda.format(estimativa.custosOperacionais)}</dd></div>
      <div><dt className="inline font-semibold">Remuneração de referência </dt><dd className="inline">{moeda.format(estimativa.remuneracao)}</dd></div>
    </dl>
  </details>;
}
