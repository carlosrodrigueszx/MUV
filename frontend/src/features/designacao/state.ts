import type { ConsultaDesignacao } from "./types";

export type EstadoConsultaDesignacao =
  | { tipo: "inicial" }
  | { tipo: "carregando"; freteId: string }
  | { tipo: "pronto"; freteId: string; dados: ConsultaDesignacao }
  | { tipo: "vazio"; freteId: string }
  | { tipo: "erro"; freteId: string; mensagem: string; conflito: boolean };

export function iniciarConsulta(freteId: string): EstadoConsultaDesignacao {
  return { tipo: "carregando", freteId };
}

export function concluirConsulta(
  estado: EstadoConsultaDesignacao,
  freteId: string,
  dados: ConsultaDesignacao | null,
): EstadoConsultaDesignacao {
  if (estado.tipo !== "carregando" || estado.freteId !== freteId) return estado;
  return dados ? { tipo: "pronto", freteId, dados } : { tipo: "vazio", freteId };
}

export function falharConsulta(
  estado: EstadoConsultaDesignacao,
  freteId: string,
  mensagem: string,
  status?: number,
): EstadoConsultaDesignacao {
  if (estado.tipo !== "carregando" || estado.freteId !== freteId) return estado;
  return { tipo: "erro", freteId, mensagem, conflito: status === 409 };
}
