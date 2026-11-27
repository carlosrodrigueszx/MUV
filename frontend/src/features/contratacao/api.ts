export type Solicitacao = {
  id: number;
  modalidade: "PESSOA" | "ENCOMENDA";
  status: string;
  origem: string;
  destino: string;
  dataPretendida: string;
};

export type Proposta = {
  id: number;
  prestador: string;
  tipoPrestador: string | null;
  avaliacao: number | null;
  autorizado: boolean;
  valor: number;
  condicoes: string;
  status: string;
};

export type Comparacao = {
  solicitacao: Solicitacao;
  propostas: Proposta[];
  freteId: number | null;
};

export type Detalhe = {
  solicitacao: Solicitacao;
  frete: {
    id: number;
    propostaId: number;
    prestador: string;
    valor: number;
    condicoes: string | null;
    contratadoEm: string;
    status: string;
    conclusao?: { realizadaEm: string; nomeRecebedor: string | null };
  };
  outrasPropostas: Proposta[];
};

async function consultar<T>(url: string, init?: RequestInit): Promise<T> {
  const resposta = await fetch(url, { cache: "no-store", ...init });
  if (!resposta.ok) {
    const mensagens: Record<number, string> = {
      401: "Entre na sua conta para continuar.",
      403: "Sua sessão não permite esta operação.",
      404: "Registro indisponível.",
      409: "A solicitação ou a proposta já foi encerrada. Atualize a página.",
    };
    throw new Error(mensagens[resposta.status] ?? "Não foi possível concluir a operação. Tente novamente.");
  }
  return resposta.json();
}

const solicitacao = (id: string) => `/api/cliente/solicitacoes/${encodeURIComponent(id)}`;

export function comparar(id: string, signal: AbortSignal) {
  return consultar<Comparacao>(`${solicitacao(id)}/propostas`, { signal });
}

export function detalhar(id: string, signal: AbortSignal) {
  return consultar<Detalhe>(`/api/cliente/fretes/${encodeURIComponent(id)}`, { signal });
}

export async function aceitar(id: string, proposta: number) {
  const csrf = await consultar<{ headerName: string; token: string }>("/api/auth/csrf");
  return consultar<{ id: number }>(`${solicitacao(id)}/propostas/${proposta}/aceite`, {
    method: "POST",
    headers: { [csrf.headerName]: csrf.token },
  });
}
