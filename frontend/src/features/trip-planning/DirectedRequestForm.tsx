import { useRef, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { EstimativaFrete, type Estimativa } from "../freight-estimate/EstimativaFrete";
import type { TripCriteria } from "./criteria";

const fields = [
  { key: "logradouro", label: "Logradouro", maxLength: 128 },
  { key: "numero", label: "Número", maxLength: 20 },
  { key: "bairro", label: "Bairro", maxLength: 128 },
  { key: "cidade", label: "Cidade", maxLength: 128 },
  { key: "estado", label: "UF", maxLength: 2, pattern: "[A-Za-z]{2}" },
  { key: "cep", label: "CEP", maxLength: 9, pattern: "[0-9]{5}-?[0-9]{3}" },
] as const;

export function DirectedRequestForm({ trip, providerId, vehicleId }: {
  trip: TripCriteria;
  providerId: number;
  vehicleId: number;
}) {
  const attempt = useRef<{ payload: string; key: string } | undefined>(undefined);
  const pending = useRef(false);
  const [busy, setBusy] = useState(false);
  const [estimativa, setEstimativa] = useState<Estimativa>();
  const [consultando, setConsultando] = useState(false);
  const [retirarOferta, setRetirarOferta] = useState(false);
  const revisaoEnderecos = useRef(0);
  const [error, setError] = useState("");
  const [requestId, setRequestId] = useState<number>();
  const points = [
    { label: "Embarque", value: trip.origin },
    ...trip.stops.map((value, index) => ({ label: `Parada ${index + 1}`, value })),
    { label: "Destino", value: trip.destination },
  ];

  function lerEnderecos(data: FormData) {
    return points.map((_, index) => Object.fromEntries(fields.map(({ key }) =>
      [key, String(data.get(`${index}.${key}`) ?? "").trim()],
    )));
  }

  async function consultarEstimativa(form: HTMLFormElement | null) {
    if (!form?.reportValidity() || consultando) return;
    const revisao = revisaoEnderecos.current;
    const addresses = lerEnderecos(new FormData(form));
    setConsultando(true);
    setError("");
    try {
      const csrf = await fetch("/api/auth/csrf", { cache: "no-store" });
      if (!csrf.ok) throw new Error();
      const { headerName, token } = await csrf.json() as { headerName: string; token: string };
      const response = await fetch("/api/cliente/solicitacoes?estimativa", {
        method: "POST",
        headers: { "Content-Type": "application/json", [headerName]: token },
        body: JSON.stringify({ origem: addresses[0], destino: addresses.at(-1), paradas: addresses.slice(1, -1) }),
      });
      if (!response.ok) throw new Error();
      const resultado = await response.json() as Estimativa;
      if (revisaoEnderecos.current === revisao) setEstimativa(resultado);
    } catch {
      if (revisaoEnderecos.current === revisao) setError("Não foi possível consultar a estimativa do frete.");
    } finally {
      setConsultando(false);
    }
  }

  const abaixoDoPiso = !retirarOferta && trip.offer != null && estimativa?.piso != null
    && Math.round(Number(trip.offer.amount) * 100) < Math.round(estimativa.piso * 100);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending.current || abaixoDoPiso) return;
    const data = new FormData(event.currentTarget);
    const addresses = lerEnderecos(data);
    const body = {
      prestadorId: providerId, veiculoId: vehicleId, passageiros: trip.passengers,
      partida: trip.departure, retorno: trip.returnAt, origem: addresses[0],
      destino: addresses.at(-1), paradas: addresses.slice(1, -1),
      valor: retirarOferta ? undefined : trip.offer?.amount,
      condicoes: retirarOferta ? undefined : trip.offer?.terms,
    };
    const payload = JSON.stringify(body);
    if (attempt.current?.payload !== payload) attempt.current = { payload, key: crypto.randomUUID() };
    pending.current = true;
    setBusy(true);
    setError("");
    try {
      const csrf = await fetch("/api/auth/csrf", { cache: "no-store" });
      if (!csrf.ok) throw new Error("Não foi possível verificar a sessão.");
      const { headerName, token } = await csrf.json() as { headerName: string; token: string };
      const response = await fetch("/api/cliente/solicitacoes", {
        method: "POST",
        headers: { "Content-Type": "application/json", [headerName]: token },
        body: JSON.stringify({ ...body, chave: attempt.current.key }),
      });
      const result = await response.json().catch(() => null) as { id?: number; detail?: string } | null;
      if (!response.ok || !result?.id) {
        throw new Error(result?.detail || (response.status === 401
          ? "Entre novamente para enviar a solicitação."
          : "Não foi possível enviar a solicitação. Confira os dados e tente novamente."));
      }
      setRequestId(result.id);
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : "Não foi possível enviar a solicitação.");
    } finally {
      pending.current = false;
      setBusy(false);
    }
  }

  if (requestId) return <div className="mt-4 space-y-2">
    <p role="status" className="font-semibold text-[#17695b]">Solicitação SL-{requestId} enviada.</p>
    <Link className="text-sm font-semibold underline" to={`/cliente/historico?solicitacao=${requestId}&periodo=${trip.departure.slice(0, 7)}#freight-details`}>
      Acompanhar em Meus fretes
    </Link>
  </div>;

  return <form onSubmit={submit} className="mt-4 grid gap-4">
    {points.map(({ label, value }, index) => (
      <fieldset key={label} disabled={busy} className="min-w-0 rounded-lg border border-slate-200 p-3">
        <legend className="px-1 text-sm font-semibold">{label} — {value}</legend>
        <div className="grid gap-3 sm:grid-cols-2">
          {fields.map(field => <label key={field.key} className="grid gap-1 text-sm">
            {field.label}
            <input name={`${index}.${field.key}`} required maxLength={field.maxLength}
              onChange={() => {
                revisaoEnderecos.current += 1;
                setEstimativa(undefined);
              }}
              pattern={"pattern" in field ? field.pattern : undefined}
              className="min-w-0 rounded-md border border-slate-300 px-3 py-2" />
          </label>)}
        </div>
      </fieldset>
    ))}
    <div className="grid gap-2 text-sm">
      <button type="button" disabled={busy || consultando} className="w-fit rounded-md border px-3 py-2"
        onClick={event => void consultarEstimativa(event.currentTarget.form)}>
        {consultando ? "Calculando…" : "Consultar valor estimado"}
      </button>
      {estimativa && <EstimativaFrete estimativa={estimativa} />}
      {estimativa?.piso != null && <p role="status" className={abaixoDoPiso ? "text-red-700" : ""}>
        {abaixoDoPiso ? "Sua oferta está abaixo do mínimo de " : "Oferta mínima de "}
        {estimativa.piso.toLocaleString("pt-BR", { style: "currency", currency: "BRL" })}.
      </p>}
      {trip.offer && <label className="flex items-center gap-2">
        <input type="checkbox" disabled={busy} checked={retirarOferta}
          onChange={event => setRetirarOferta(event.target.checked)} />
        Enviar sem a oferta inicial
      </label>}
    </div>
    {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
    <button disabled={busy || abaixoDoPiso} className="w-fit cursor-pointer rounded-lg bg-[#17695b] px-4 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60">
      {busy ? "Enviando…" : "Enviar solicitação direcionada"}
    </button>
  </form>;
}
