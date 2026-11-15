import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { LuBell, LuChevronRight, LuHeadset, LuRoute } from "react-icons/lu";
import { Avatar } from "../ui/Avatar";
import { providerNavItems, type ProviderNavItem } from "./providerNav";

interface ProviderLayoutProps {
  children: ReactNode;
  /** Texto final do breadcrumb (página atual). */
  pageTitle: string;
  activeNavKey: string;
  organizationName?: string;
  organizationVerified?: boolean;
  avatarUrl?: string | null;
  isOnline?: boolean;
  onOnlineChange?: (online: boolean) => void;
  onNotificationsClick?: () => void;
  /** Contadores por `key` do item de navegação (ex.: { requests: 3 }). */
  navBadges?: Record<string, number>;
  navItems?: ProviderNavItem[];
  supportHref?: string;
  passengerPortalHref?: string;
}

export function ProviderLayout({
  children,
  pageTitle,
  activeNavKey,
  organizationName = "",
  organizationVerified = false,
  avatarUrl,
  isOnline = false,
  onOnlineChange,
  onNotificationsClick,
  navBadges = {},
  navItems = providerNavItems,
  supportHref = "#",
  passengerPortalHref = "#",
}: ProviderLayoutProps) {
  return (
    <div
      className="flex min-h-screen bg-[#f5f7f8] text-[#1d2d32] antialiased"
      style={{ fontFamily: "Inter, ui-sans-serif, system-ui, sans-serif" }}
    >
      {/* Navegação do prestador */}
      <aside className="sticky top-0 flex h-screen w-[232px] shrink-0 flex-col gap-[30px] overflow-y-auto border-r border-[#dfe6e7] bg-white pt-7 pr-5 pb-6 pl-5">
        <Link to="/" className="flex items-center gap-[9px]">
          <span className="flex h-8 w-8 items-center justify-center rounded-[10px] bg-[#17695b] text-white">
            <LuRoute size={20} />
          </span>
          <span className="text-[25px] leading-[30px] font-bold tracking-[-0.01em] text-[#104e43]">
            MUV
          </span>
        </Link>

        <span className="w-fit rounded-full bg-[#eff3f4] px-2.5 py-[5px] text-[11px] leading-4 font-semibold text-[#637479]">
          PORTAL DO PRESTADOR
        </span>

        <nav aria-label="Portal do prestador" className="flex flex-col gap-[7px]">
          {navItems.map(({ key, label, to, icon: Icon }) => {
            const active = key === activeNavKey;
            const badge = navBadges[key];
            return (
              <Link
                key={key}
                to={to}
                aria-current={active ? "page" : undefined}
                className={`flex h-[46px] items-center gap-3 rounded-[10px] px-[13px] text-[13px] leading-[19px] font-medium transition-colors ${
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

        <div className="flex flex-col gap-2.5 rounded-xl bg-[#f5f7f8] p-[15px]">
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

        <div className="flex flex-col gap-3.5">
          <div className="h-px bg-[#dfe6e7]" />
          <div className="flex items-center gap-2.5">
            <Avatar src={avatarUrl} alt="" size={36} />
            <div className="flex min-w-0 flex-col gap-0.5">
              <span className="truncate text-[12px] leading-[17px] font-semibold text-[#1d2d32]">
                {organizationName}
              </span>
              <span className="text-[11px] leading-4 text-[#637479]">
                {organizationVerified ? "Empresa · Verificada" : "Empresa"}
              </span>
            </div>
          </div>
          <a
            href={passengerPortalHref}
            className="text-[11px] leading-4 text-[#637479] hover:underline"
          >
            Ir para o portal do passageiro ↗
          </a>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        {/* Barra superior */}
        <header className="flex h-20 shrink-0 items-center justify-between bg-white px-9">
          <nav aria-label="Navegação estrutural" className="flex items-center gap-2.5 text-[13px] leading-[19px]">
            <span className="text-[#637479]">Área do prestador</span>
            <LuChevronRight size={14} className="text-[#637479]" />
            <span className="font-semibold text-[#1d2d32]">{pageTitle}</span>
          </nav>

          <div className="flex items-center gap-[18px]">
            <button
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
            </button>
            <span className="h-[22px] w-px bg-[#dfe6e7]" />
            <button
              type="button"
              aria-label="Notificações"
              onClick={onNotificationsClick}
              className="text-[#1d2d32]"
            >
              <LuBell size={20} />
            </button>
            <Avatar src={avatarUrl} alt="" size={32} />
          </div>
        </header>

        <main className="px-9 pt-9 pb-9">{children}</main>
      </div>
    </div>
  );
}
