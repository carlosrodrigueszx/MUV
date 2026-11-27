import { useEffect, useRef, useState, type FormEvent } from "react";
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

type RequestProps = { providerId: number } & (
  | { trip: TripCriteria; vehicleId: number }
  | { trip?: never; vehicleId?: never }
);

const addressInput = "min-w-0 rounded-md border border-slate-300 px-3 py-2";

export function DirectedRequestForm({ trip, providerId, vehicleId }: RequestProps) {
  const attempt = useRef<{ payload: string; key: string } | undefined>(undefined);
  const pending = useRef(false);
  const [busy, setBusy] = useState(false);
  const [estimativa, setEstimativa] = useState<Estimativa>();
  const [consultando, setConsultando] = useState(false);
  const [retirarOferta, setRetirarOferta] = useState(false);
  const revisaoEnderecos = useRef(0);
  const [error, setError] = useState("");
  const [receipt, setReceipt] = useState<{ id: number; month: string }>();
  const receiptMessage = useRef<HTMLParagraphElement>(null);
  useEffect(() => {
    if (receipt) receiptMessage.current?.focus();
  }, [receipt]);
  const points = [
    { label: trip ? "Embarque" : "Coleta", value: trip?.origin },
    ...(trip?.stops ?? []).map((value, index) => ({ label: `Parada ${index + 1}`, value })),
    { label: trip ? "Destino" : "Entrega", value: trip?.destination },
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

  const abaixoDoPiso = !retirarOferta && trip?.offer != null && estimativa?.piso != null
    && Math.round(Number(trip.offer.amount) * 100) < Math.round(estimativa.piso * 100);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending.current || abaixoDoPiso) return;
    const data = new FormData(event.currentTarget);
    const addresses = lerEnderecos(data);
    const value = (name: string) => String(data.get(name) ?? "").trim();
    const partida = trip?.departure ?? value("partida");
    const contato = (tipo: string) => ({
      nome: value(`${tipo}.nome`), telefone: value(`${tipo}.telefone`), instrucoes: value(`${tipo}.instrucoes`),
    });
    const body = trip ? {
      prestadorId: providerId, veiculoId: vehicleId, passageiros: trip.passengers,
      partida, retorno: trip.returnAt, origem: addresses[0],
      destino: addresses.at(-1), paradas: addresses.slice(1, -1),
      valor: retirarOferta ? undefined : trip.offer?.amount,
      condicoes: retirarOferta ? undefined : trip.offer?.terms,
    } : {
      prestadorId: providerId, partida, origem: addresses[0], destino: addresses[1],
      descricaoVolumes: value("descricaoVolumes"), pesoKg: Number(value("pesoKg")),
      alturaCm: Number(value("alturaCm")), larguraCm: Number(value("larguraCm")),
      profundidadeCm: Number(value("profundidadeCm")), coleta: contato("coleta"), entrega: contato("entrega"),
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
      const response = await fetch(trip ? "/api/cliente/solicitacoes" : "/api/cliente/solicitacoes/encomendas", {
        method: "POST",
        headers: { "Content-Type": "application/json", [headerName]: token },
        body: JSON.stringify({ ...body, chave: attempt.current.key }),
      });
      const result = await response.json().catch(() => null) as { id?: number; detail?: string } | null;
      if (!response.ok || !result?.id) {
        const mensagens: Record<number, string> = {
          400: "Confira a data de coleta, os endereços e os dados obrigatórios da solicitação.",
          401: "Sua sessão terminou. Entre novamente para enviar a solicitação.",
          403: "O envio exige uma sessão válida de cliente. Entre novamente na sua conta.",
          409: "A disponibilidade do prestador mudou. Volte à busca e selecione um prestador elegível.",
        };
        throw new Error(result?.detail || mensagens[response.status]
          || "O servidor não confirmou o envio. Confira Meus fretes antes de tentar novamente.");
      }
      setReceipt({ id: result.id, month: partida.slice(0, 7) });
    } catch (failure) {
      setError(failure instanceof TypeError
        ? "A conexão com o servidor falhou. Tente novamente para confirmar o envio."
        : failure instanceof Error ? failure.message : "Não foi possível enviar a solicitação.");
    } finally {
      pending.current = false;
      setBusy(false);
    }
  }

  if (receipt) return <div className="mt-4 space-y-2 rounded-lg border border-[#bfd9d1] bg-[#e5f2ee] p-4 text-[#104e43]">
    <p role="status" tabIndex={-1} ref={receiptMessage}>
      <strong>Solicitação SL-{receipt.id} enviada.</strong> Aguarde a resposta do prestador.
    </p>
    <Link className="inline-flex min-h-11 cursor-pointer items-center font-semibold underline focus-visible:outline-2 focus-visible:outline-offset-2" to={`/cliente/historico?solicitacao=${receipt.id}&periodo=${receipt.month}#freight-details`}>
      Acompanhar em Meus fretes
    </Link>
  </div>;

  return <form onSubmit={submit} className="mt-4 grid gap-4">
    {!trip && <fieldset disabled={busy} className="grid gap-3 rounded-lg border border-slate-200 p-3 sm:grid-cols-2">
      <legend className="px-1 text-sm font-semibold">Dados da encomenda</legend>
      <label className="grid gap-1 text-sm">Data e hora da coleta (horário de Fortaleza)
        <input className={addressInput} name="partida" type="datetime-local" required />
      </label>
      <label className="grid gap-1 text-sm">Quantidade e descrição dos volumes
        <input className={addressInput} name="descricaoVolumes" maxLength={255} required />
      </label>
      {[
        ["pesoKg", "Peso total (kg)", "99999999.99"],
        ["alturaCm", "Altura (cm)", "99999.99"],
        ["larguraCm", "Largura (cm)", "99999.99"],
        ["profundidadeCm", "Profundidade (cm)", "99999.99"],
      ].map(([name, label, max]) => <label className="grid gap-1 text-sm" key={name}>{label}
        <input className={addressInput} name={name} type="number" min="0.01" max={max} step="0.01" required />
      </label>)}
    </fieldset>}
    {points.map(({ label, value }, index) => (
      <fieldset key={label} disabled={busy} className="min-w-0 rounded-lg border border-slate-200 p-3">
        <legend className="px-1 text-sm font-semibold">{label}{value && ` — ${value}`}</legend>
        <div className="grid gap-3 sm:grid-cols-2">
          {fields.map(field => <label key={field.key} className="grid gap-1 text-sm">
            {field.label}
            <input name={`${index}.${field.key}`} required maxLength={field.maxLength}
              onChange={() => {
                revisaoEnderecos.current += 1;
                setEstimativa(undefined);
              }}
              pattern={"pattern" in field ? field.pattern : undefined}
              title={field.key === "cep" ? "Informe oito dígitos, com ou sem hífen."
                : field.key === "estado" ? "Informe a sigla de duas letras, por exemplo CE." : undefined}
              className="min-w-0 rounded-md border border-slate-300 px-3 py-2" />
          </label>)}
        </div>
        {!trip && <div className="mt-3 grid gap-3 sm:grid-cols-2">
          <label className="grid gap-1 text-sm">Nome do contato
            <input className={addressInput} name={`${index === 0 ? "coleta" : "entrega"}.nome`} maxLength={100} required />
          </label>
          <label className="grid gap-1 text-sm">Telefone do contato
            <input className={addressInput} name={`${index === 0 ? "coleta" : "entrega"}.telefone`} type="tel" maxLength={20} required />
          </label>
          <label className="grid gap-1 text-sm sm:col-span-2">Instruções (opcional)
            <textarea className={addressInput} name={`${index === 0 ? "coleta" : "entrega"}.instrucoes`} maxLength={255} />
          </label>
        </div>}
      </fieldset>
    ))}
    {trip && <div className="grid gap-2 text-sm">
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
    </div>}
    {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
    <button disabled={busy || abaixoDoPiso} className="w-fit cursor-pointer rounded-lg bg-[#17695b] px-4 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60">
      {busy ? "Enviando…" : "Enviar solicitação direcionada"}
    </button>
  </form>;
}
