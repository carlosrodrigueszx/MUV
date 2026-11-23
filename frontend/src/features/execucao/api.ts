export type AcaoExecucao =
  | "INICIO"
  | "OCORRENCIA"
  | "INTERRUPCAO"
  | "RETOMADA"
  | "ENCERRAMENTO_SEM_CONCLUSAO";

export type RecursosExecucao = {
  motorista: string;
  veiculo: string;
  placa: string;
};

export type EventoExecucao = {
  id: number;
  tipo: AcaoExecucao;
  autor: string;
  descricao: string | null;
  justificativa: string | null;
  horarioRegistro: string | null;
};

export type Acompanhamento = {
  freteId: number;
  modalidade: "PESSOA" | "ENCOMENDA";
  estado: string;
  revisaoDesignacao: number;
  recursos: RecursosExecucao | null;
  cicloExecucaoId: number | null;
  interrupcaoId: number | null;
  eventos: EventoExecucao[];
  acoesElegiveis: AcaoExecucao[];
  prestadorResponsavel: boolean;
  usuarioAutenticado: string;
};

const API = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/+$/, "");

export async function consultarExecucao(freteId: string, signal: AbortSignal): Promise<Acompanhamento> {
  const resposta = await fetch(`${API}/api/fretes/${encodeURIComponent(freteId)}/execucao`, {
    credentials: "include",
    cache: "no-store",
    signal,
  });
  if (resposta.ok) return resposta.json() as Promise<Acompanhamento>;
  switch (resposta.status) {
    case 401:
      throw new Error("Entre na sua conta para acompanhar este frete.");
    case 403:
      throw new Error("Você não tem acesso a este frete.");
    case 404:
      throw new Error("Frete não encontrado.");
    default:
      throw new Error("Não foi possível consultar o frete. Tente novamente.");
  }
}

export type AcaoTransicao = "INTERRUPCAO" | "RETOMADA" | "ENCERRAMENTO_SEM_CONCLUSAO";

export type ComandoTransicao = {
  chaveOperacao: string;
  estadoEsperado: string;
  revisaoDesignacao: number;
  justificativa: string;
  data: string;
  horario: string;
} & ({ cicloExecucaoId: number } | { interrupcaoId: number; confirmou: boolean; situacaoEncomenda?: string });

export type TransicaoConfirmada = {
  eventoId: number;
  autorId: number;
  estadoAnterior: "EM_EXECUCAO" | "INTERROMPIDO";
  estadoResultante: "INTERROMPIDO" | "EM_EXECUCAO" | "ENCERRADO_SEM_CONCLUSAO";
};

export class ErroTransicao extends Error {
  readonly status: number;

  constructor(status: number) {
    const mensagens: Record<number, string> = {
      400: "Verifique a data, o horário, a justificativa e os campos de confirmação.",
      401: "Entre na sua conta para confirmar a ação.",
      403: "Sua sessão ou designação não permite confirmar esta ação. Atualize o acompanhamento.",
      404: "Frete não encontrado.",
      409: "A confirmação entrou em conflito com o estado do frete. Atualize o acompanhamento.",
    };
    super(mensagens[status] ?? "Não foi possível verificar o resultado da confirmação.");
    this.status = status;
  }
}

export async function registrarTransicao(
  freteId: number, acao: AcaoTransicao, comando: ComandoTransicao,
): Promise<TransicaoConfirmada> {
  const signal = AbortSignal.timeout(15_000);
  const csrf = await fetch(`${API}/api/auth/csrf`, {
    credentials: "include", cache: "no-store", signal,
  });
  if (!csrf.ok) throw new ErroTransicao(csrf.status);
  const { headerName, token } = await csrf.json() as { headerName: string; token: string };

  const recursos: Record<AcaoTransicao, string> = {
    INTERRUPCAO: "interrupcoes",
    RETOMADA: "retomadas",
    ENCERRAMENTO_SEM_CONCLUSAO: "encerramento-sem-conclusao",
  };
  const recurso = recursos[acao];
  const resposta = await fetch(`${API}/api/fretes/${freteId}/execucao/${recurso}`, {
    method: "POST",
    credentials: "include",
    cache: "no-store",
    headers: { "Content-Type": "application/json", [headerName]: token },
    body: JSON.stringify(comando),
    signal,
  });
  if (!resposta.ok) throw new ErroTransicao(resposta.status);
  return resposta.json() as Promise<TransicaoConfirmada>;
}
