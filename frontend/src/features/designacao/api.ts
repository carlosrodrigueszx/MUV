import type {
  ComandoSubstituicao,
  ConsultaDesignacao,
  DesignacaoConfirmada,
  SubstituicaoConfirmada,
} from "./types";

export class DesignacaoApiError extends Error {
  readonly status: number;

  constructor(
    message: string,
    status: number,
  ) {
    super(message);
    this.status = status;
  }
}

interface CsrfResponse {
  headerName: string;
  token: string;
}

const apiBase = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/+$/, "");

async function erroDaResposta(resposta: Response) {
  const corpo = await resposta.json().catch(() => null) as
    | { detail?: string; message?: string }
    | null;
  const mensagens: Record<number, string> = {
    401: "Entre na sua conta para continuar.",
    403: "Você não pode acessar este frete.",
    404: "Frete não encontrado.",
    409: "O frete foi atualizado. Recarregue os dados antes de confirmar.",
    422: "Os recursos selecionados não são compatíveis com este frete.",
  };
  return new DesignacaoApiError(
    corpo?.detail ?? corpo?.message ?? mensagens[resposta.status]
      ?? "Não foi possível concluir a operação.",
    resposta.status,
  );
}

async function consultar<T>(url: string, init?: RequestInit): Promise<T> {
  const resposta = await fetch(`${apiBase}${url}`, {
    credentials: "include",
    cache: "no-store",
    ...init,
  });
  if (!resposta.ok) throw await erroDaResposta(resposta);
  return resposta.json() as Promise<T>;
}

async function csrf(): Promise<CsrfResponse> {
  return consultar<CsrfResponse>("/api/auth/csrf");
}

const recurso = (freteId: string | number) =>
  `/api/prestador/fretes/${encodeURIComponent(String(freteId))}`;

export function consultarDesignacao(freteId: string, signal: AbortSignal) {
  return consultar<ConsultaDesignacao>(`${recurso(freteId)}/designacao`, { signal });
}

export async function designar(
  freteId: string,
  motoristaId: number,
  veiculoId: number,
  revisao: number,
) {
  const token = await csrf();
  return consultar<DesignacaoConfirmada>(`${recurso(freteId)}/designacao`, {
    method: "POST",
    headers: { "Content-Type": "application/json", [token.headerName]: token.token },
    body: JSON.stringify({ motoristaId, veiculoId, revisao }),
  });
}

export async function substituir(freteId: string, comando: ComandoSubstituicao) {
  const token = await csrf();
  return consultar<SubstituicaoConfirmada>(`${recurso(freteId)}/substituicoes`, {
    method: "POST",
    headers: { "Content-Type": "application/json", [token.headerName]: token.token },
    body: JSON.stringify(comando),
  });
}
