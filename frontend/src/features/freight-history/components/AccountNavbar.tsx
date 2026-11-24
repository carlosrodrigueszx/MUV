import { AccountMenu } from "../../auth/AccountMenu";
import { useEffect, useState } from "react";
import { Link, NavLink, useLocation } from "react-router-dom";
import bellIcon from "../../../assets/figma/bell.svg";
import routeIcon from "../../../assets/figma/route.svg";
import { getSession, readRecentSession, type Session } from "../../auth/api";

export function AccountNavbar({ currentSession, fixed = true }: {
  currentSession?: Session;
  fixed?: boolean;
}) {
  const { pathname } = useLocation();
  const [loadedSession, setLoadedSession] = useState<Session | null | undefined>(readRecentSession);
  const session = currentSession ?? loadedSession;
  const portalHome = session === null ? "/"
    : session?.passageiro || session === undefined ? "/cliente/planejar-viagem"
    : session.empresa ? "/prestador"
    : session.motorista || session.prestador ? "/motorista" : "/conta";
  const links = [
    { label: "Planejar viagem", to: "/cliente/planejar-viagem" },
    { label: "Prestadores", to: "/cliente/prestadores" },
    { label: "Meus fretes", to: pathname.startsWith("/prestador/") ? "/prestador/historico" : "/cliente/historico" },
  ];
  useEffect(() => {
    if (currentSession) return;
    const controller = new AbortController();
    getSession(controller.signal).then(account => {
      if (!controller.signal.aborted) setLoadedSession(account);
    }).catch(() => {
      if (!controller.signal.aborted) setLoadedSession(null);
    });
    return () => controller.abort();
  }, [currentSession]);

  return <>
    <header className={`border-b border-slate-200 bg-white ${fixed ? "fixed inset-x-0 top-0 z-50" : "relative z-20"}`}>
      <nav aria-label="Menu principal" className="mx-auto grid max-w-[1440px] grid-cols-[1fr_auto] items-center gap-x-4 px-4 sm:px-8 lg:min-h-20 lg:grid-cols-[1fr_auto_1fr] lg:px-12">
        <Link to={portalHome} aria-label="Início do MUV" className="flex w-fit items-center gap-2.5 py-4 font-bold text-[#005447]">
          <span className="grid size-8 place-items-center rounded-[10px] bg-[#007464]"><img src={routeIcon} alt="" width={20} height={20} /></span>
          <span className="text-[25px] leading-none tracking-tight">MUV</span>
        </Link>
        <div className="col-span-2 row-start-2 flex flex-wrap items-stretch justify-center gap-x-6 text-sm font-medium text-[#5c717a] sm:gap-x-9 lg:col-span-1 lg:col-start-2 lg:row-start-1 lg:min-h-20">
          {links.map(({ to, label }) => (
            <NavLink key={to} to={to} end className={({ isActive }) =>
              `inline-flex items-center border-b-2 py-3 hover:text-[#005d50] ${isActive
                ? "border-[#007464] font-semibold text-[#005d50]"
                : "border-transparent"}`
            }>{label}</NavLink>
          ))}
        </div>
        <div className="col-start-2 row-start-1 flex items-center justify-self-end gap-3 sm:gap-5 lg:col-start-3">
          {session?.podeAtivarAutonomo && pathname !== "/conta" && (
            <Link to="/conta#seja-motorista"
              className="whitespace-nowrap text-xs font-medium text-[#5c717a] hover:text-[#005d50]">
              Seja motorista
            </Link>
          )}
          <span className="h-6 w-px bg-[#dfe6e7]" aria-hidden="true" />
          <button type="button" aria-label="Notificações" disabled className="grid size-5 place-items-center">
            <img src={bellIcon} alt="" width={20} height={20} />
          </button>
          <AccountMenu session={session} showProfessionalPortal />
        </div>
      </nav>
    </header>

  </>;
}
