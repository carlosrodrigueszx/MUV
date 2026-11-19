import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import { FreightCancellationHeader } from "./FreightCancellationHeader";
import { cancelFreight, CancellationError, getCancellation, type CancellationDetails } from "./api";

const container = "mx-auto w-full max-w-[1440px] px-[clamp(16px,3.333vw,48px)]";
const card = "min-w-0 rounded-[14px] border border-[#dce4e5] bg-white p-4 sm:p-[22px]";
const cardTitle = "text-lg font-semibold leading-7";
const fieldLabel = "block text-xs font-semibold leading-[18px] text-[#56696e]";
const field = "mt-1.5 min-h-11 rounded-lg border border-[#dce4e5] bg-[#f4f6f7] px-3 py-[11px] text-sm leading-5 text-[#1d2d32] [overflow-wrap:anywhere]";
const action = "inline-flex min-h-[38px] items-center justify-center rounded-lg border px-4 py-2 text-center text-xs font-semibold leading-5";
const secondary = `${action} border-[#dce4e5] bg-white text-[#17695b] hover:bg-[#f4f6f7]`;
const banner = "mt-[22px] rounded-[14px] bg-[#fff3df] p-[15px] text-[13px] leading-5";
const eligible = (status: string) => ["AGUARDANDO_DESIGNACAO", "PRONTO_INICIO"].includes(status);

function CancellationForm({ id }: { id: string }) {
  const [data, setData] = useState<CancellationDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [sessionExpired, setSessionExpired] = useState(false);
  const [reason, setReason] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [refreshWarning, setRefreshWarning] = useState("");
  const sending = useRef(false);
  const mounted = useRef(false);

  function showError(cause: unknown) {
    setError(cause instanceof Error ? cause.message : "Não foi possível carregar o frete.");
    setSessionExpired(cause instanceof CancellationError && cause.status === 401);
  }

  useEffect(() => {
    mounted.current = true;
    const controller = new AbortController();
    getCancellation(id, controller.signal).then(setData).catch((cause: unknown) => {
      if (!controller.signal.aborted) showError(cause);
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => { mounted.current = false; controller.abort(); };
  }, [id]);

  async function refresh() {
    try {
      const updated = await getCancellation(id);
      if (mounted.current) { setData(updated); setRefreshWarning(""); }
    } catch {
      if (mounted.current) setRefreshWarning("Não foi possível atualizar o registro. Recarregue os dados para consultar o estado atual.");
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (sending.current || !confirmed || !reason.trim() || !data || !eligible(data.frete.status)) return;
    sending.current = true;
    setSubmitting(true);
    setError("");
    try {
      await cancelFreight(id, reason.trim());
      if (!mounted.current) return;
      setSuccess(true);
      setData({ ...data, frete: { ...data.frete, status: "CANCELADO" } });
      await refresh();
    } catch (cause) {
      if (!mounted.current) return;
      showError(cause);
      // Reconcile stale state (including a response lost after the server committed).
      await refresh();
    } finally {
      sending.current = false;
      if (mounted.current) setSubmitting(false);
    }
  }

  const freight = data?.frete;
  const canCancel = freight && eligible(freight.status) && !success && !sessionExpired;
  const back = data?.autorPapel === "Prestador" ? "/prestador/historico" : "/cliente/historico";

  return <>
    <FreightCancellationHeader containerClass={container} historyRoute={back} name={sessionExpired ? undefined : data?.autorNome} />
    <main className={`${container} pb-9 pt-8 lg:pt-[74px]`}>
      <p className="text-[11px] font-bold leading-4 text-[#17695b]">RF011 / MUV</p>
      <h1 className="mt-4 text-[26px] font-bold leading-tight tracking-tight lg:mt-6 lg:text-3xl lg:leading-10">{freight?.status === "CANCELADO" ? "Frete cancelado" : "Precisa cancelar este frete?"}</h1>
      {loading && <p role="status" className="mt-6">Carregando frete…</p>}
      {error && <div role="alert" className="mt-6 rounded-xl bg-red-50 p-4 text-red-800">{error} {sessionExpired && <Link to="/entrar" className="underline">Entrar</Link>}</div>}
      {success && <p role="status" className="mt-6 rounded-xl bg-[#E5F2EF] p-4 text-[#176A5B]">Frete cancelado com sucesso. A outra parte foi notificada.</p>}
      {refreshWarning && <div role="alert" className="mt-4 rounded-xl bg-amber-50 p-4 text-amber-900">{refreshWarning} <button type="button" className="underline" onClick={() => void refresh()}>Recarregar dados</button></div>}
      {data && freight && <>
        <p className="mt-4 text-sm leading-5 text-[#56696e] [overflow-wrap:anywhere] lg:mt-[22px]">FR-{freight.id} · {freight.status === "CANCELADO" ? "Cancelado" : eligible(freight.status) ? "Contratado" : "Execução iniciada"} · {freight.modalidade === "ENCOMENDA" ? "Encomendas" : "Pessoas"} · {freight.origem} → {freight.destino} · {new Date(freight.dataHoraPretendida).toLocaleString("pt-BR", { dateStyle: "medium", timeStyle: "short" })}</p>
        {canCancel ? <div className={banner}>
          <p className="text-sm font-semibold text-[#875b16]">O serviço ainda não começou</p>
          <p className="mt-0.5">O cliente ou o prestador responsável pode cancelar antes do início, com justificativa obrigatória.</p>
        </div> : !success && <p role="status" className={banner}>{freight.status === "CANCELADO" ? "Este frete já foi cancelado. Consulte o registro abaixo." : sessionExpired ? "Entre novamente para continuar." : "Este frete já iniciou e não pode ser cancelado. Após o início, registre uma ocorrência ou um encerramento sem conclusão."}</p>}
        <div className="mt-[22px] grid items-start gap-4 md:grid-cols-2 lg:gap-5">
          <section className={card}>
            <h2 className={cardTitle}>{canCancel ? "Informe o motivo" : "Justificativa do cancelamento"}</h2>
            {canCancel ? <form onSubmit={submit} className="mt-3">
              <label className={fieldLabel} htmlFor="justificativa">Justificativa do cancelamento *</label>
              <textarea id="justificativa" name="justificativa" required rows={1} value={reason}
                disabled={submitting} onChange={(event) => setReason(event.target.value)} aria-describedby="reason-help"
                className={`${field} block max-h-48 w-full resize-y field-sizing-content overflow-y-auto focus:border-[#17695b] focus:outline-2 focus:outline-offset-2 focus:outline-[#17695b]`} />
              <p id="reason-help" className="mt-3 text-[13px] leading-[18px] text-[#56696e]">O motivo será compartilhado com a outra parte e ficará no histórico.</p>
              <label className="mt-3.5 flex items-start gap-1.5 text-sm font-semibold leading-5">
                <input type="checkbox" checked={confirmed} disabled={submitting} onChange={(event) => setConfirmed(event.target.checked)} className="mt-[3px] size-3.5 shrink-0 accent-[#17695b]" />
                Entendo que este frete será cancelado.
              </label>
              <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2">
                <button type="submit" disabled={submitting || !confirmed || !reason.trim()} className={`${action} border-transparent bg-[#ad4040] text-white hover:bg-[#933636] disabled:cursor-not-allowed disabled:opacity-50`}>{submitting ? "Cancelando…" : "Confirmar cancelamento"}</button>
                {!submitting && <Link to={back} className={secondary}>Manter frete</Link>}
              </div>
            </form> : <>
              {data.registro && <p className="mt-3 whitespace-pre-wrap text-sm [overflow-wrap:anywhere]">{data.registro.justificativa}</p>}
              <Link to={back} className={`${secondary} mt-5`}>Voltar para meus fretes</Link>
            </>}
          </section>
          <section className={card}>
            <h2 className={cardTitle}>Registro do cancelamento</h2>
            <dl className="mt-3 space-y-3.5">
              <div><dt className={fieldLabel}>{data.registro ? "Registrado por" : "Solicitado por"}</dt><dd className={field}>{data.registro?.autor ?? `${data.autorNome} · ${data.autorPapel}`}</dd></div>
              <div><dt className={fieldLabel}>Prestador responsável</dt><dd className={field}>{freight.prestadorNome}</dd></div>
              <div><dt className={fieldLabel}>Data e horário</dt><dd className={field}>{data.registro ? new Date(`${data.registro.dataHora}Z`).toLocaleString("pt-BR") : "Registrados automaticamente na confirmação"}</dd></div>
            </dl>
            <div className="mt-3.5 rounded-[14px] bg-[#e4f1ed] p-[15px] text-[13px] leading-5">
              <p className="text-sm font-semibold text-[#104e43]">Sem aplicação automática de multa</p>
              <p className="mt-0.5">Não há cobrança ou reembolso automático. A outra parte será notificada.</p>
            </div>
            <p className="mt-3.5 text-xs leading-[18px] text-[#56696e]">Após o início, registre uma ocorrência ou um encerramento sem conclusão em vez de cancelar.</p>
          </section>
        </div>
      </>}
    </main>
  </>;
}

export default function FreightCancellationPage() {
  const { freteId = "" } = useParams();
  return <div className="min-h-screen bg-[#f4f6f7] text-[#1d2d32] [font-family:Inter,'Noto_Sans',Arial,sans-serif]">
    <CancellationForm key={freteId} id={freteId} />
  </div>;
}
