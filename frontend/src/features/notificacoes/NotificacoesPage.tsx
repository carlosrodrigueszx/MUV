import { useCallback, useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { LuBell, LuCheck, LuExternalLink } from "react-icons/lu";
import { ProviderLayout } from "../../components/layout/ProviderLayout";
import { AccountNavbar } from "../freight-history/components/AccountNavbar";
import { getSession } from "../auth/api";
import {
  listarNotificacoes,
  marcarNotificacaoComoLida,
  marcarTodasComoLidas,
  NotificacoesApiError,
  type CentralNotificacoes,
} from "./api";

type Filtro = "TODAS" | "NAO_LIDAS";

const dateFormat = new Intl.DateTimeFormat("pt-BR", {
  day: "2-digit",
  month: "short",
  hour: "2-digit",
  minute: "2-digit",
});

function formatarData(value: string) {
  const normalized = value.length === 19 ? `${value}Z` : value;
  const date = new Date(normalized);
  return Number.isNaN(date.valueOf()) ? "Data indisponível" : dateFormat.format(date).replace(" de ", " ");
}

export default function NotificacoesPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const prestador = location.pathname.startsWith("/prestador/");
  const [filtro, setFiltro] = useState<Filtro>("TODAS");
  const [central, setCentral] = useState<CentralNotificacoes | null>(null);
  const [loading, setLoading] = useState(true);
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [pendingAll, setPendingAll] = useState(false);
  const [error, setError] = useState("");

  const carregar = useCallback((signal?: AbortSignal) => {
    setLoading(true);
    setError("");
    return listarNotificacoes(filtro, signal)
      .then(setCentral)
      .catch((reason: unknown) => {
        if (reason instanceof DOMException && reason.name === "AbortError") return;
        if (reason instanceof NotificacoesApiError && reason.status === 401) {
          navigate("/entrar", { replace: true, state: { notice: reason.message } });
          return;
        }
        setError(reason instanceof Error ? reason.message : "Não foi possível carregar suas notificações.");
      })
      .finally(() => setLoading(false));
  }, [filtro, navigate]);

  useEffect(() => {
    const controller = new AbortController();
    getSession(controller.signal)
      .then((session) => {
        if (!session) {
          navigate("/entrar", { replace: true });
          return;
        }
        void carregar(controller.signal);
      })
      .catch((reason: unknown) => {
        if (reason instanceof DOMException && reason.name === "AbortError") return;
        setLoading(false);
        setError("Não foi possível verificar sua sessão. Tente novamente.");
      });
    return () => controller.abort();
  }, [carregar, navigate]);

  async function marcarUma(id: number) {
    if (!central || pendingId !== null) return;
    setPendingId(id);
    setError("");
    try {
      await marcarNotificacaoComoLida(id);
      setCentral(await listarNotificacoes(filtro));
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Não foi possível atualizar a notificação.");
    } finally {
      setPendingId(null);
    }
  }

  async function marcarTodas() {
    if (!central || central.naoLidas === 0 || pendingAll) return;
    setPendingAll(true);
    setError("");
    try {
      await marcarTodasComoLidas();
      setCentral(await listarNotificacoes(filtro));
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Não foi possível atualizar as notificações.");
    } finally {
      setPendingAll(false);
    }
  }

  const inbox = (
    <section className="mx-auto w-full max-w-[1344px]" aria-labelledby="notifications-title">
      <p className="text-[11px] font-bold leading-4 text-[#17695b]">RF013 / MUV</p>
      <h1 id="notifications-title" className="mt-7 text-[30px] leading-[1.45] font-bold text-[#1d2d32]">
        Tudo que mudou, em um só lugar
      </h1>
      <p className="mt-5 text-sm leading-5 text-[#637479]">
        Acompanhe propostas, contratações e alterações dos fretes relacionados à sua conta.
      </p>

      <div className="mt-7 flex flex-wrap items-center gap-3 sm:gap-5">
        <button type="button" aria-pressed={filtro === "TODAS"} onClick={() => setFiltro("TODAS")}
          className={`h-[42px] rounded-lg px-[18px] text-[13px] font-semibold transition-colors ${filtro === "TODAS" ? "bg-[#17695b] text-white" : "border border-[#dfe7e8] bg-white text-[#17695b] hover:bg-[#f8fbfa]"}`}>
          Todas
        </button>
        <button type="button" aria-pressed={filtro === "NAO_LIDAS"} onClick={() => setFiltro("NAO_LIDAS")}
          className={`h-[42px] rounded-lg px-[18px] text-[13px] font-semibold transition-colors ${filtro === "NAO_LIDAS" ? "bg-[#17695b] text-white" : "border border-[#dfe7e8] bg-white text-[#17695b] hover:bg-[#f8fbfa]"}`}>
          Não lidas ({central?.naoLidas ?? 0})
        </button>
        <button type="button" onClick={() => void marcarTodas()} disabled={!central?.naoLidas || pendingAll}
          className="h-[42px] rounded-lg border border-[#dfe7e8] bg-white px-[18px] text-[13px] font-semibold text-[#17695b] transition-colors hover:bg-[#f8fbfa] disabled:cursor-not-allowed disabled:opacity-50">
          {pendingAll ? "Atualizando…" : "Marcar todas como lidas"}
        </button>
      </div>

      {error && <p role="alert" className="mt-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}
      {loading ? (
        <p role="status" className="mt-6 rounded-[14px] border border-[#dfe7e8] bg-white p-8 text-sm text-[#637479]">Carregando notificações…</p>
      ) : !central?.itens.length ? (
        <div className="mt-6 rounded-[14px] border border-[#dfe7e8] bg-white px-6 py-12 text-center">
          <LuBell className="mx-auto text-[#17695b]" size={24} aria-hidden="true" />
          <h2 className="mt-3 text-base font-semibold text-[#1d2d32]">Nenhuma notificação por aqui</h2>
          <p className="mt-2 text-sm text-[#637479]">
            {filtro === "NAO_LIDAS" ? "Todas as suas notificações estão lidas." : "Quando houver novidades, elas aparecerão nesta central."}
          </p>
        </div>
      ) : (
        <ul className="mt-6 space-y-6" aria-label="Notificações">
          {central.itens.map((item) => (
            <li key={item.id}>
              <article className={`rounded-[14px] border bg-white p-6 sm:p-6 ${item.lida ? "border-[#dfe7e8]" : "border-[#c7dfd8]"}`}>
                <div className="flex flex-col gap-5 sm:flex-row sm:items-start sm:justify-between">
                  <div className="min-w-0 flex-1">
                    <p className={`text-[11px] leading-4 font-semibold ${item.lida ? "text-[#637479]" : "text-[#17695b]"}`}>
                      {!item.lida && <span className="font-bold">NOVA · </span>}{formatarData(item.criadoEm)}
                    </p>
                    <h2 className="mt-3 text-[17px] leading-6 font-semibold text-[#1d2d32]">{item.titulo}</h2>
                    <p className="mt-3 text-[13px] leading-[19px] text-[#637479]">{item.mensagem}</p>
                    <div className="mt-4 flex flex-wrap items-center gap-4">
                      {item.acaoUrl && item.acaoRotulo && (
                        <Link to={item.acaoUrl} className="inline-flex h-[42px] items-center gap-2 rounded-lg border border-[#dfe7e8] px-[18px] text-[13px] font-semibold text-[#17695b] hover:bg-[#f8fbfa]">
                          {item.acaoRotulo}<LuExternalLink size={15} aria-hidden="true" />
                        </Link>
                      )}
                      {!item.lida && (
                        <button type="button" onClick={() => void marcarUma(item.id)} disabled={pendingId !== null}
                          className="inline-flex min-h-10 items-center gap-2 rounded-lg px-2 text-[12px] font-semibold text-[#637479] underline decoration-[#c5d0d2] underline-offset-4 hover:text-[#17695b] disabled:opacity-50">
                          <LuCheck size={15} aria-hidden="true" />
                          {pendingId === item.id ? "Atualizando…" : "Marcar como lida"}
                        </button>
                      )}
                    </div>
                  </div>
                  <span className={`mt-1 size-2 shrink-0 rounded-full ${item.lida ? "bg-transparent" : "bg-[#17695b]"}`} aria-label={item.lida ? "Lida" : "Não lida"} />
                </div>
              </article>
            </li>
          ))}
        </ul>
      )}
    </section>
  );

  if (prestador) {
    return <ProviderLayout pageTitle="Notificações" activeNavKey="notifications">{inbox}</ProviderLayout>;
  }

  return (
    <div className="min-h-screen bg-[#f5f7f8] text-[#1d2d32] antialiased [font-family:Inter,'Noto_Sans',Arial,sans-serif]">
      <AccountNavbar />
      <main className="mx-auto max-w-[1440px] px-5 pb-10 pt-28 sm:px-8 lg:px-12">{inbox}</main>
    </div>
  );
}
