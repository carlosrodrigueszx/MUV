import { useEffect, useState } from "react";
import { Link, Navigate, useLocation } from "react-router-dom";
import { LuArrowRight, LuCalendarDays, LuCircleCheck, LuRoute, LuStar, LuWallet } from "react-icons/lu";
import { ProviderLayout } from "../components/layout/ProviderLayout";
import { getSession, readRecentSession, type Session } from "../features/auth/api";
import type { Item } from "../features/solicitacoes/Disponiveis";

type Overview = {
  completed: number;
  active: number;
  amount: number | null;
  rating: number | null;
  chart: { month: string; completed: number }[];
  upcoming: { departure: string; origin: string; destination: string }[];
};

const box = "min-w-0 rounded-2xl border border-[#dfe6e7] bg-white p-5 shadow-sm";
const action = "inline-flex min-h-10 items-center gap-2 rounded-lg border border-[#dfe6e7] bg-white px-3 text-sm font-semibold text-[#17695b] hover:bg-[#e5f2ee]";
const currency = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const date = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" });
const monthName = new Intl.DateTimeFormat("pt-BR", { month: "short" });

async function read<T>(url: string, signal: AbortSignal): Promise<T> {
  const response = await fetch(url, { signal, cache: "no-store" });
  if (!response.ok) throw new Error("Não foi possível carregar estes dados.");
  return response.json() as Promise<T>;
}

export default function PortalOverview({ driver = false }: { driver?: boolean }) {
  const [session, setSession] = useState<Session | null | undefined>(readRecentSession);
  const [overview, setOverview] = useState<Overview>();
  const [requests, setRequests] = useState<Item[]>();
  const [sessionError, setSessionError] = useState("");
  const [overviewError, setOverviewError] = useState("");
  const [requestsError, setRequestsError] = useState("");
  const allowed = driver
    ? session?.motorista || (session?.prestador && !session.empresa)
    : session?.empresa;
  const { hash, key: locationKey } = useLocation();

  useEffect(() => {
    if (allowed && hash === "#agenda") {
      document.getElementById("agenda")?.scrollIntoView();
    }
  }, [allowed, hash, locationKey, overview]);

  useEffect(() => {
    const controller = new AbortController();
    getSession(controller.signal).then(account => {
      if (!controller.signal.aborted) setSession(account);
    }).catch(() => {
      if (!controller.signal.aborted) setSessionError("Não foi possível confirmar a sessão.");
    });
    return () => controller.abort();
  }, []);

  useEffect(() => {
    if (!allowed) return;
    const controller = new AbortController();
    const portal = driver ? "MOTORISTA" : "PRESTADOR";
    read<Overview>(`/api/auth/visao-geral?portal=${portal}`, controller.signal).then(setOverview).catch(() => {
      if (!controller.signal.aborted) setOverviewError("Não foi possível carregar a visão geral.");
    });
    if (!driver) {
      read<Item[]>("/api/prestador/solicitacoes", controller.signal)
        .then(items => setRequests(items.filter(item => item.autorizada))).catch(() => {
          if (!controller.signal.aborted) setRequestsError("Não foi possível carregar as solicitações.");
        });
    }
    return () => controller.abort();
  }, [allowed, driver, session?.id]);

  if (session === null) return <Navigate to="/entrar" replace />;

  if (!driver && session && !session.empresa && (session.motorista || session.prestador)) {
    return <Navigate to="/motorista" replace />;
  }

  const metrics = [
    { label: "Fretes concluídos no mês", value: overview?.completed, icon: LuCircleCheck },
    { label: "Fretes ativos", value: overview?.active, icon: LuRoute },
    ...(!driver ? [
      { label: "Valor acordado nos fretes concluídos no mês", value: overview && currency.format(overview.amount ?? 0), icon: LuWallet },
      { label: "Avaliação média", value: overview?.rating?.toLocaleString("pt-BR", { maximumFractionDigits: 2 }), icon: LuStar },
    ] : []),
  ];
  const chartMax = Math.max(1, ...(overview?.chart.map(item => item.completed) ?? []));
  const hasActivity = overview?.chart.some(item => item.completed > 0);

  return <ProviderLayout pageTitle="Visão geral" activeNavKey="overview" isOnline={null}
    organizationName={session?.nome} profileLabel={driver ? "Motorista" : "Empresa"}
    navBadges={requests ? { requests: requests.length } : undefined}
    portalLabel={driver ? "PORTAL DO MOTORISTA" : undefined} areaLabel={driver ? "Área do motorista" : undefined}>
    {sessionError ? <p role="alert">{sessionError}</p> : !session ? <p role="status">Verificando sessão…</p>
      : !allowed ? <p role="alert">Esta conta não tem acesso a este portal.</p> : (
      <div className="mx-auto max-w-7xl space-y-5">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <p className="text-xs font-semibold text-[#17695b]">
              {new Date().toLocaleDateString("pt-BR", { dateStyle: "full" })}
            </p>
            <h1 className="mt-1 text-2xl font-bold tracking-tight sm:text-3xl">Olá, {session.nome}!</h1>
          </div>
          <Link to="#agenda" className={action}><LuCalendarDays aria-hidden size={16} />Ver agenda</Link>
        </div>

        {overviewError && <p role="alert" className={box}>{overviewError}</p>}
        {!overview && !overviewError && <p role="status">Carregando indicadores…</p>}
        <div className={`grid grid-cols-1 gap-3 sm:grid-cols-2 ${driver ? "" : "xl:grid-cols-4"}`}>
          {metrics.map(({ label, value, icon: Icon }) => <div className={box} key={label}>
            <div className="flex items-start justify-between gap-2 text-sm text-slate-600">
              <p>{label}</p><Icon aria-hidden size={18} className="shrink-0 text-[#17695b]" />
            </div>
            <p className="mt-2 break-words text-2xl font-bold">{value ?? "—"}</p>
          </div>)}
        </div>

        {!driver && <section className={box} aria-labelledby="overview-requests">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 id="overview-requests" className="font-bold">Solicitações disponíveis</h2>
            <Link to="/prestador/solicitacoes" className={action}>Ver todas <LuArrowRight aria-hidden size={16} /></Link>
          </div>
          {requestsError ? <p role="alert" className="mt-4 text-sm text-slate-600">{requestsError}</p>
            : !requests ? <p role="status" className="mt-4 text-sm text-slate-600">Carregando solicitações…</p>
            : requests.length === 0 ? <p className="mt-4 text-sm text-slate-600">Nenhuma solicitação nas suas modalidades autorizadas.</p>
            : <ul className="mt-3 divide-y divide-slate-100">{requests.slice(0, 3).map(item => (
              <li key={item.id} className="flex flex-wrap items-center justify-between gap-2 py-3">
                <div>
                  <p className="font-semibold">{item.origemCidade} → {item.destinoCidade}</p>
                  <p className="text-sm text-slate-600">{date.format(new Date(item.partida))} · {item.modalidade === "PESSOA" ? "Pessoas" : "Encomendas"}</p>
                </div>
                <Link to={`/prestador/solicitacoes/${item.id}/avaliacao`} className={action}>Avaliar</Link>
              </li>
            ))}</ul>}
        </section>}

        <div className="grid gap-4 lg:grid-cols-[3fr_2fr]">
          <section className={box} aria-labelledby="overview-chart">
            <h2 id="overview-chart" className="font-bold">Fretes concluídos por mês</h2>
            {overview && (hasActivity ? <ul className="mt-5 grid grid-cols-6 items-end gap-2">
              {overview.chart.map(({ month, completed }) => <li key={month} className="min-w-0 text-center text-xs text-slate-600">
                <p>{completed}</p>
                <div aria-hidden className="mt-1 rounded-t bg-[#b7dbcf] last-of-type:bg-[#b7dbcf]"
                  style={{ height: `${Math.max(2, completed / chartMax * 88)}px` }} />
                <p className="mt-2">{monthName.format(new Date(`${month}-01T12:00:00`))}</p>
              </li>)}
            </ul> : <p className="mt-4 text-sm text-slate-600">Nenhum frete concluído nos últimos seis meses.</p>)}
          </section>
          <section id="agenda" className={box} aria-labelledby="overview-agenda">
            <h2 id="overview-agenda" className="font-bold">Na sua agenda</h2>
            {overview && (overview.upcoming.length ? <ul className="mt-3 divide-y divide-slate-100">
              {overview.upcoming.map((item, index) => <li key={`${item.departure}-${index}`} className="py-3">
                <p className="text-sm font-semibold">{item.origin} → {item.destination}</p>
                <time dateTime={item.departure} className="text-xs text-slate-600">{date.format(new Date(item.departure))}</time>
              </li>)}
            </ul> : <p className="mt-4 text-sm text-slate-600">Nenhum frete futuro agendado.</p>)}
          </section>
        </div>
      </div>
    )}
  </ProviderLayout>;
}
