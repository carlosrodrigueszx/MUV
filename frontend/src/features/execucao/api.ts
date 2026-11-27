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
  tipo: AcaoExecucao | "CONCLUSAO";
  autor: string;
  descricao: string | null;
  justificativa: string | null;
  horarioRegistro: string | null;
  horarioInformado: string | null;
  localOcorrencia: string | null;
  situacaoEncomenda: string | null;
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
  acoesElegiveis: (AcaoExecucao | "CONCLUSAO")[];
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

export type AcaoTransicao = "INICIO" | "OCORRENCIA" | "INTERRUPCAO" | "RETOMADA" | "ENCERRAMENTO_SEM_CONCLUSAO";

export type ComandoInicio = {
  chaveOperacao: string;
  estadoEsperado: string;
  revisaoDesignacao: number;
};

export type ComandoTransicao = ComandoInicio & {
  data: string;
  horario: string;
} & ({ descricao: string } | { justificativa: string })
  & ({ cicloExecucaoId: number; localOcorrencia?: string } | { interrupcaoId: number; confirmou: boolean; situacaoEncomenda?: string });

export type TransicaoConfirmada = {
  eventoId: number;
  autorId: number;
  estadoAnterior: "PRONTO_INICIO" | "EM_EXECUCAO" | "INTERROMPIDO";
  estadoResultante: "INTERROMPIDO" | "EM_EXECUCAO" | "ENCERRADO_SEM_CONCLUSAO";
};

export class ErroTransicao extends Error {
  readonly status: number;

  constructor(status: number, detalhe?: string) {
    const mensagens: Record<number, string> = {
      400: "Verifique a data, o horário, a justificativa e os campos de confirmação.",
      401: "Entre na sua conta para confirmar a ação.",
      403: "Sua sessão ou designação não permite confirmar esta ação. Atualize o acompanhamento.",
      404: "Frete não encontrado.",
      409: "A confirmação entrou em conflito com o estado do frete. Atualize o acompanhamento.",
    };
    super(detalhe ?? mensagens[status] ?? "Não foi possível verificar o resultado da confirmação.");
    this.status = status;
  }
}

export async function registrarTransicao(
  freteId: number, acao: AcaoTransicao, comando: ComandoTransicao | ComandoInicio,
): Promise<TransicaoConfirmada> {
  const recursos: Record<AcaoTransicao, string> = {
    INICIO: "inicio",
    OCORRENCIA: "ocorrencias",
    INTERRUPCAO: "interrupcoes",
    RETOMADA: "retomadas",
    ENCERRAMENTO_SEM_CONCLUSAO: "encerramento-sem-conclusao",
  };
  const resposta = await postarOperacao(freteId, recursos[acao], comando);
  if (!resposta.ok) {
    const problema = await resposta.json().catch(() => null) as { detail?: string } | null;
    throw new ErroTransicao(resposta.status, problema?.detail);
  }
  return resposta.json() as Promise<TransicaoConfirmada>;
}

export async function concluirFrete(freteId: number, comando: {
  revisaoDesignacao: number;
  cicloExecucaoId: number | null;
  data?: string;
  horario?: string;
  nomeRecebedor?: string;
  cpfRecebedor?: string;
}): Promise<void> {
  const resposta = await postarOperacao(freteId, "conclusao", comando);
  if (resposta.ok) return;
  const problema = await resposta.json().catch(() => null) as { detail?: string } | null;
  const mensagens: Record<number, string> = {
    400: "Os dados da entrega não puderam ser lidos. Confira o formato da data e da hora.",
    401: "Sua sessão terminou. Entre novamente para confirmar a conclusão.",
    403: "A confirmação exige uma sessão válida do motorista designado. Volte ao acompanhamento.",
    404: "O frete não foi encontrado.",
    409: "O frete foi atualizado. Volte ao acompanhamento e tente novamente.",
  };
  throw new Error(problema?.detail ?? mensagens[resposta.status]
    ?? "O servidor não confirmou a conclusão. Confira o histórico antes de tentar novamente.");
}

async function postarOperacao(freteId: number, recurso: string, comando: object): Promise<Response> {
  const signal = AbortSignal.timeout(15_000);
  const csrf = await fetch(`${API}/api/auth/csrf`, {
    credentials: "include", cache: "no-store", signal,
  });
  if (!csrf.ok) throw new ErroTransicao(csrf.status);
  const { headerName, token } = await csrf.json() as { headerName: string; token: string };

  return fetch(`${API}/api/fretes/${freteId}/execucao/${recurso}`, {
    method: "POST",
    credentials: "include",
    cache: "no-store",
    headers: { "Content-Type": "application/json", [headerName]: token },
    body: JSON.stringify(comando),
    signal,
  });
}
