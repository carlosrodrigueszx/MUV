import type { IconType } from "react-icons";
import {
  LuCalendarDays,
  LuCarFront,
  LuInbox,
  LuLayoutDashboard,
  LuMap,
  LuWallet,
} from "react-icons/lu";

export interface ProviderNavItem {
  key: string;
  label: string;
  to: string;
  icon: IconType;
}

/**
 * TODO(rotas): caminhos provisórios — alinhar com o Router quando as
 * demais telas do portal do prestador existirem.
 */
export const providerNavItems: ProviderNavItem[] = [
  { key: "overview", label: "Visão geral", to: "/prestador", icon: LuLayoutDashboard },
  { key: "requests", label: "Solicitações", to: "/prestador/solicitacoes", icon: LuInbox },
  { key: "freights", label: "Fretes", to: "/prestador/fretes", icon: LuCalendarDays },
  { key: "vehicles", label: "Veículos", to: "/prestador/veiculos", icon: LuCarFront },
  { key: "drivers", label: "Motoristas", to: "/prestador/motoristas", icon: LuMap },
  { key: "history", label: "Histórico", to: "/prestador/historico", icon: LuWallet },
];
