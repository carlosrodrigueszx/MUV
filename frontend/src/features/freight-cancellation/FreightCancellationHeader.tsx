import { useState } from "react";
import { LuMenu, LuUserRound, LuX } from "react-icons/lu";
import { Link } from "react-router-dom";
import bellIcon from "../../assets/figma/bell.svg";
import chevronDownIcon from "../../assets/figma/chevron-down.svg";
import routeIcon from "../../assets/figma/route.svg";

type HeaderProps = { containerClass: string; historyRoute: string; name?: string };

export function FreightCancellationHeader({ containerClass, historyRoute, name }: HeaderProps) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [notice, setNotice] = useState("");
  const links = [
    { label: "Solicitar frete", to: "/cliente/planejar-viagem" },
    { label: "Prestadores", to: "/cliente/prestadores" },
    { label: "Meus fretes", to: historyRoute },
  ];
  const initials = name?.trim().split(/\s+/).filter(Boolean)
    .map((word) => word[0]).filter((_, index, words) => index === 0 || index === words.length - 1)
    .join("").toLocaleUpperCase("pt-BR");

  function showProfileNotice() {
    setNotice("A troca de perfil ainda não está disponível.");
  }

  return (
    <header className="relative z-10 border-b border-[#dfe6e7] bg-white">
      <div className={`${containerClass} grid h-14 grid-cols-[1fr_auto] items-center gap-3 lg:h-[74px] lg:grid-cols-[1fr_auto_1fr]`}>
        <Link to={historyRoute.startsWith("/prestador/") ? "/prestador" : "/cliente/planejar-viagem"} className="flex w-fit items-center gap-2 text-[#104e43]" aria-label="Página inicial MUV">
          <span className="grid size-8 place-items-center rounded-[9px] bg-[#17695b]">
            <img src={routeIcon} alt="" width={20} height={20} />
          </span>
          <span className="text-2xl font-bold leading-none tracking-tight">MUV</span>
        </Link>

        <nav aria-label="Navegação principal" className="hidden h-full items-center gap-8 text-sm text-[#56696e] lg:flex">
          {links.map(({ label, to }, index) => (
            <Link key={to} to={to} className={`relative flex h-full items-center hover:text-[#17695b] ${
              index === 0 ? "font-semibold after:absolute after:inset-x-0 after:bottom-0 after:h-0.5 after:bg-[#17695b]" : "font-medium"
            }`}>
              {label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center justify-end gap-2.5 text-[#56696e] lg:gap-4">
          <button type="button" onClick={showProfileNotice} className="hidden whitespace-nowrap text-xs font-medium hover:text-[#17695b] lg:block">
            Trocar perfil
          </button>
          <span aria-hidden="true" className="hidden h-5 w-px bg-[#dfe6e7] lg:block" />
          <button type="button" aria-label="Notificações"
            onClick={() => setNotice("A central de notificações ainda não está disponível.")}
            className="grid size-9 place-items-center rounded-lg hover:bg-[#f4f6f7] lg:size-6">
            <img src={bellIcon} alt="" width={18} height={18} />
          </button>
          <Link to="/conta" aria-label="Minha conta" title={name || "Minha conta"} className="flex items-center gap-3 rounded-lg">
            <span className="grid size-9 place-items-center rounded-full bg-[#e5f2ee] text-xs font-semibold text-[#17695b]">
              {initials || <LuUserRound size={17} aria-hidden="true" />}
            </span>
            <img src={chevronDownIcon} alt="" width={14} height={14} className="hidden lg:block" />
          </Link>
          <button type="button" aria-label={menuOpen ? "Fechar menu" : "Abrir menu"} aria-expanded={menuOpen}
            aria-controls="rf011-mobile-navigation" onClick={() => { setMenuOpen(!menuOpen); setNotice(""); }}
            className="grid size-9 place-items-center rounded-lg hover:bg-[#f4f6f7] lg:hidden">
            {menuOpen ? <LuX size={20} /> : <LuMenu size={20} />}
          </button>
        </div>
      </div>

      <nav id="rf011-mobile-navigation" aria-label="Navegação principal"
        className={`${containerClass} border-t border-[#dfe6e7] py-2 text-sm text-[#56696e] lg:hidden ${menuOpen ? "" : "hidden"}`}>
        {links.map(({ label, to }) => (
          <Link key={to} to={to} className="block rounded-lg px-2 py-2.5 hover:bg-[#f4f6f7]">{label}</Link>
        ))}
        <button type="button" onClick={showProfileNotice} className="w-full rounded-lg px-2 py-2.5 text-left hover:bg-[#f4f6f7]">
          Trocar perfil
        </button>
      </nav>

      <div className={`${containerClass} absolute inset-x-0 top-full`}>
        <div role="status" className={notice ? "ml-auto mt-2 flex w-fit max-w-full items-center gap-3 rounded-lg border border-[#dfe6e7] bg-white px-3 py-2 text-sm shadow-sm" : "sr-only"}>
          {notice}
          {notice && (
            <button type="button" onClick={() => setNotice("")} aria-label="Fechar aviso" className="grid size-7 shrink-0 place-items-center rounded-md hover:bg-[#f4f6f7]">
              <LuX size={16} />
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
