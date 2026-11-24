import { useState, type Ref } from "react";
import { Link, useNavigate } from "react-router-dom";
import { LuLogOut } from "react-icons/lu";
import chevronDownIcon from "../../assets/figma/chevron-down.svg";
import { Avatar } from "../../components/ui/Avatar";
import { authPost, type Session } from "./api";

export function AccountMenu({ session, menuRef, avatarUrl, showProfessionalPortal = false }: {
  session: Session | null | undefined;
  menuRef?: Ref<HTMLDetailsElement>;
  avatarUrl?: string | null;
  showProfessionalPortal?: boolean;
}) {
  const navigate = useNavigate();
  const [loggingOut, setLoggingOut] = useState(false);
  const [logoutError, setLogoutError] = useState("");
  async function logout() {
    if (loggingOut) return;
    setLoggingOut(true);
    setLogoutError("");
    try {
      await authPost("logout");
      navigate("/entrar", { replace: true });
    } catch {
      setLogoutError("Não foi possível sair. Tente novamente.");
    } finally {
      setLoggingOut(false);
    }
  }

  return (<>
    {session ? <details ref={menuRef} className="group/account relative" onKeyDown={event => {
      if (event.key === "Escape" && event.currentTarget.open) {
        event.currentTarget.open = false;
        event.currentTarget.querySelector("summary")?.focus();
      }
    }}>
      <summary aria-label="Menu da conta" title={session.nome}
        className="inline-flex min-h-11 cursor-pointer list-none items-center gap-1 rounded-full pr-1.5 hover:bg-[#f0f7f4] focus-visible:outline-2 focus-visible:outline-[#005d50] [&::-webkit-details-marker]:hidden">
        <Avatar src={avatarUrl} name={session.nome} size={36} />
        <img src={chevronDownIcon} alt="" width={12} height={12} className="shrink-0 transition-transform group-open/account:rotate-180" />
      </summary>
      <div className="absolute right-0 z-20 mt-1.5 w-60 max-w-[calc(100vw-2rem)] overflow-hidden rounded-xl border border-slate-200 bg-white shadow-lg">
        <div className="border-b border-slate-100 bg-[#f5faf8] px-4 py-3">
          <p className="text-xs font-medium text-slate-600">Minha conta</p>
          <p className="mt-1 break-words text-sm font-semibold text-[#005d50]">{session.nome}</p>
          <p className="break-all text-xs text-slate-600">{session.email}</p>
        </div>
        <div className="p-1.5">
          {showProfessionalPortal && session.passageiro && session.motorista && (
            <Link to="/motorista"
              className="flex min-h-11 items-center rounded-lg px-2.5 text-sm font-semibold text-[#005d50] hover:bg-[#edf6f2]">
              Ir para o portal do motorista
            </Link>
          )}
          {showProfessionalPortal && session.passageiro && session.prestador && (
            <Link to="/prestador"
              className="flex min-h-11 items-center rounded-lg px-2.5 text-sm font-semibold text-[#005d50] hover:bg-[#edf6f2]">
              {session.empresa ? "Ir para o portal da empresa" : "Ir para o portal do prestador"}
            </Link>
          )}
          <button type="button" onClick={logout} disabled={loggingOut}
            className="flex min-h-11 w-full items-center gap-2.5 rounded-lg px-2.5 text-left text-sm font-semibold text-[#005d50] hover:bg-[#edf6f2] focus-visible:outline-2 focus-visible:outline-inset focus-visible:outline-[#005d50] disabled:opacity-60">
            <LuLogOut aria-hidden="true" size={16} />
            {loggingOut ? "Saindo…" : "Sair"}
          </button>
        </div>
        {logoutError && <p role="alert" className="px-3 py-2 text-xs text-red-800">{logoutError}</p>}
      </div>
    </details> : session === null && (
      <Link to="/entrar" className="text-sm font-semibold text-[#005d50]">Entrar</Link>
    )}
  </>);
}
