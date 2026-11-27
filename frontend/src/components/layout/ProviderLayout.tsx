import { AccountMenu } from "../../features/auth/AccountMenu";
import { getSession, readRecentSession, type Session } from "../../features/auth/api";
import { useEffect, useState, type ReactNode } from "react";
import { Link, useLocation } from "react-router-dom";
import { LuBell, LuChevronRight, LuClipboardList, LuHeadset, LuRoute } from "react-icons/lu";
import { Avatar } from "../ui/Avatar";
import { driverNavItems, providerNavItems, type ProviderNavItem } from "./providerNav";

interface ProviderLayoutProps {
  children: ReactNode;
  /** Texto final do breadcrumb (página atual). */
  pageTitle: string;
  activeNavKey: string;
  organizationName?: string;
  organizationVerified?: boolean;
  profileLabel?: string;
  avatarUrl?: string | null;
  isOnline?: boolean | null;
  onOnlineChange?: (online: boolean) => void;
  /** Contadores por `key` do item de navegação (ex.: { requests: 3 }). */
  navBadges?: Record<string, number>;
  navItems?: ProviderNavItem[];
  supportHref?: string;
  passengerPortalHref?: string;
  portalLabel?: string;
  areaLabel?: string;
}

export function ProviderLayout({
  children,
  pageTitle,
  activeNavKey,
  organizationName = "",
  organizationVerified = false,
  profileLabel,
  avatarUrl,
  isOnline = false,
  onOnlineChange,
  navBadges = {},
  navItems = providerNavItems,
  supportHref = "#",
  passengerPortalHref = "/cliente/planejar-viagem",
  portalLabel = "PORTAL DO PRESTADOR",
  areaLabel = "Área do prestador",
}: ProviderLayoutProps) {
  const { pathname, hash } = useLocation();
  const portalHome = pathname === "/motorista" || pathname.startsWith("/motorista/")
    ? "/motorista" : "/prestador";
  const [session, setSession] = useState<Session | null | undefined>(readRecentSession);
  const companyPortal = session?.empresa && portalLabel === "PORTAL DO PRESTADOR";
  const inDriverPortal = portalHome === "/motorista";
  const displayPortalLabel = inDriverPortal ? "PORTAL DO MOTORISTA" : companyPortal ? "PORTAL DA EMPRESA" : portalLabel;

  useEffect(() => {
    const controller = new AbortController();
    getSession(controller.signal).then(account => {
      if (!controller.signal.aborted) setSession(account);
    }).catch(() => {
      if (!controller.signal.aborted) setSession(null);
    });
    return () => controller.abort();
  }, []);

  const providerId = session?.prestador ? session.id : undefined;
  const requestPath = providerId && (!inDriverPortal || !session?.empresa)
    ? `${portalHome}/solicitacoes` : undefined;
  const [requestCount, setRequestCount] = useState(0);
  const areaHome = portalLabel === "PORTAL DO MOTORISTA" ? "/motorista" : portalHome;
  const autonomous = inDriverPortal && session?.prestador && !session.empresa;
  const portalNav = (inDriverPortal ? driverNavItems : navItems).map(item => {
    if (autonomous && item.key === "requests") return { ...item, to: "/motorista/solicitacoes" };
    if (autonomous && item.key === "vehicles") return { ...item, to: "/motorista/veiculos", label: "Veículos" };
    return item;
  });
  const section = portalNav.find(item => item.key === activeNavKey);
  const sectionPath = section?.to;
  const parentSection = sectionPath && sectionPath !== areaHome && pathname.startsWith(sectionPath + "/")
    ? section : undefined;
  const breadcrumbLink = "cursor-pointer rounded-sm hover:text-[#17695b] hover:underline focus-visible:outline-2 focus-visible:outline-offset-2";


  useEffect(() => {
    if (!requestPath || providerId === undefined) return;
    let request: AbortController | undefined;
    async function refresh() {
      request?.abort();
      const controller = new AbortController();
      request = controller;
      try {
        const response = await fetch("/api/prestador/solicitacoes", {
          signal: controller.signal, cache: "no-store",
        });
        if (!response.ok) return;
        const rows = await response.json() as Array<{ autorizada: boolean; respondida: boolean }>;
        if (!controller.signal.aborted) setRequestCount(rows.filter(row => row.autorizada && !row.respondida).length);
      } catch {
        // Keep the last count while the connection is unavailable.
      }
    }
    void refresh();
    const timer = window.setInterval(() => { if (!document.hidden) void refresh(); }, 15000);
    window.addEventListener("focus", refresh);
    window.addEventListener("muv:solicitacao-respondida", refresh);
    return () => {
      request?.abort();
      window.clearInterval(timer);
      window.removeEventListener("focus", refresh);
      window.removeEventListener("muv:solicitacao-respondida", refresh);
    };
  }, [providerId, requestPath, pathname]);

  return (
    <div
      className="flex min-h-screen flex-col bg-[#f5f7f8] text-[#1d2d32] antialiased md:flex-row"
      style={{ fontFamily: "Inter, ui-sans-serif, system-ui, sans-serif" }}
    >
      {/* Navegação do prestador */}
      <aside className="flex w-full shrink-0 flex-col gap-4 overflow-hidden border-b border-[#dfe6e7] bg-white px-4 py-4 md:sticky md:top-0 md:h-screen md:w-[232px] md:gap-[30px] md:overflow-y-auto md:border-r md:border-b-0 md:pt-7 md:pr-5 md:pb-6 md:pl-5">
        <Link to={portalHome} aria-label="Início do portal MUV" className="flex items-center gap-[9px]">
          <span className="flex h-8 w-8 items-center justify-center rounded-[10px] bg-[#17695b] text-white">
            <LuRoute size={20} />
          </span>
          <span className="text-[25px] leading-[30px] font-bold tracking-[-0.01em] text-[#104e43]">
            MUV
          </span>
        </Link>

        <span className="hidden w-fit rounded-full bg-[#eff3f4] px-2.5 py-[5px] text-[11px] leading-4 font-semibold text-[#637479] md:inline-flex">
          {displayPortalLabel}
        </span>

        <nav aria-label={displayPortalLabel} className="flex gap-[7px] overflow-x-auto pb-1 md:flex-col md:overflow-visible md:pb-0">
          {portalNav.map(({ key, label, to, icon: Icon }) => {
            if (!to) {
              return (
                <span key={key} aria-disabled="true"
                  className="flex min-h-[46px] shrink-0 items-center gap-3 rounded-[10px] px-[13px] py-2 text-[13px] text-slate-500">
                  <Icon size={19} className="shrink-0" aria-hidden="true" />
                  <span>{label}<small className="block">Em breve</small></span>
                </span>
              );
            }
            const anchorNav = portalHome === "/motorista" && activeNavKey === "overview"
              ? driverNavItems.find((item) => hash && item.to === portalHome + hash)
              : undefined;
            const currentKey = anchorNav?.key ?? activeNavKey;
            const active = key === currentKey;
            const badge = key === "requests" && requestPath ? requestCount : navBadges[key];
            return (
              <Link
                key={key}
                to={to}
                aria-current={active ? (anchorNav ? "location" : "page") : undefined}
                className={`flex h-[46px] shrink-0 items-center gap-3 rounded-[10px] px-[13px] text-[13px] leading-[19px] font-medium transition-colors ${
                  active
                    ? "bg-[#e5f2ee] text-[#17695b]"
                    : "text-[#637479] hover:bg-[#f5f7f8]"
                }`}
              >
                <Icon size={19} className="shrink-0" />
                <span>{label}</span>
                {badge ? (
                  <span className="flex h-[19px] min-w-[19px] items-center justify-center rounded-[6px] bg-[#17695b] px-1.5 text-[10px] leading-[15px] text-white">
                    {badge}
                  </span>
                ) : null}
              </Link>
            );
          })}
        </nav>

        <div className="hidden flex-col gap-2.5 rounded-xl bg-[#f5f7f8] p-[15px] md:flex">
          <LuHeadset size={20} className="text-[#17695b]" />
          <p className="text-[13px] leading-[19px] font-semibold text-[#1d2d32]">
            Conte com a gente
          </p>
          <p className="text-[11px] leading-4 text-[#637479]">
            Suporte humano para você seguir tranquilo.
          </p>
          <a
            href={supportHref}
            className="text-[12px] leading-[17px] font-semibold text-[#17695b] hover:underline"
          >
            Falar com o suporte ↗
          </a>
        </div>

        <div className="hidden flex-col gap-3.5 md:flex">
          <div className="h-px bg-[#dfe6e7]" />
          <div className="flex items-center gap-2.5">
            <Avatar src={avatarUrl} name={session?.nome || organizationName} size={36} />
            <div className="flex min-w-0 flex-col gap-0.5">
              <span className="truncate text-[12px] leading-[17px] font-semibold text-[#1d2d32]">
                {organizationName || session?.nome}
              </span>
              <span className="text-[11px] leading-4 text-[#637479]">
                {inDriverPortal ? "Motorista" : profileLabel ?? (organizationVerified ? "Empresa · Verificada" : "Empresa")}
              </span>
            </div>
          </div>
          {session?.passageiro && (
            <Link to={passengerPortalHref} className="text-[11px] leading-4 text-[#637479] hover:underline">
              Ir para o portal do passageiro ↗
            </Link>
          )}
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        {/* Barra superior */}
        <header className="flex min-h-20 shrink-0 flex-wrap items-center justify-between gap-3 bg-white px-4 py-4 sm:px-6 md:h-20 md:px-9 md:py-0">
          <nav aria-label="Navegação estrutural" className="flex flex-wrap items-center gap-2.5 text-[13px] leading-[19px]">
            <Link to={areaHome} className={`text-[#637479] ${breadcrumbLink}`}>
              {areaHome === "/motorista" ? "Área do motorista" : companyPortal ? "Área da empresa" : areaLabel}
            </Link>
            <LuChevronRight aria-hidden="true" size={14} className="shrink-0 text-[#637479]" />
            {parentSection?.to && (
              <>
                <Link to={parentSection.to} className={`text-[#637479] ${breadcrumbLink}`}>
                  {parentSection.key === "requests" ? "Solicitações abertas" : parentSection.label}
                </Link>
                <LuChevronRight aria-hidden="true" size={14} className="shrink-0 text-[#637479]" />
              </>
            )}
            {sectionPath === pathname && sectionPath !== areaHome
              ? <Link to={sectionPath} aria-current="page" className={`font-semibold text-[#1d2d32] ${breadcrumbLink}`}>{pageTitle}</Link>
              : <span aria-current="page" className="font-semibold text-[#1d2d32]">{pageTitle}</span>}
          </nav>

          <div className="flex items-center gap-[18px]">
            {isOnline !== null && <button
              type="button"
              role="switch"
              aria-checked={isOnline}
              onClick={() => onOnlineChange?.(!isOnline)}
              className="flex items-center gap-2"
            >
              <span
                className={`flex h-[21px] w-9 items-center rounded-full p-[3px] transition-colors ${
                  isOnline ? "justify-end bg-[#17695b]" : "justify-start bg-[#c5d0d2]"
                }`}
              >
                <span className="h-[15px] w-[15px] rounded-full bg-white" />
              </span>
              <span
                className={`text-[12px] leading-[17px] font-semibold ${
                  isOnline ? "text-[#17695b]" : "text-[#637479]"
                }`}
              >
                {isOnline ? "Você está online" : "Você está offline"}
              </span>
            </button>}
            <span className="h-[22px] w-px bg-[#dfe6e7]" />
            {requestPath && (
              <Link to={requestPath} aria-label={`Solicitações pendentes: ${requestCount}`}
                className="relative grid size-9 place-items-center rounded-lg hover:bg-[#f5f7f8]">
                <LuClipboardList size={20} aria-hidden />
                {requestCount > 0 && <span className="absolute right-0 top-0 rounded-full bg-[#17695b] px-1 text-[10px] text-white">{requestCount}</span>}
              </Link>
            )}
            <Link to={inDriverPortal ? "/motorista/notificacoes" : "/prestador/notificacoes"}
              aria-label="Notificações" className="grid size-9 place-items-center rounded-lg hover:bg-[#f5f7f8]">
              <LuBell size={20} />
            </Link>
            <AccountMenu session={session} avatarUrl={avatarUrl} />
          </div>
        </header>

        <main className="px-4 pt-6 pb-9 sm:px-6 md:px-9 md:pt-9">{children}</main>
      </div>
    </div>
  );
}
