export type Notificacao = {
  id: number;
  titulo: string;
  mensagem: string;
  criadoEm: string;
  lida: boolean;
  freteId: number | null;
  solicitacaoId: number | null;
  acaoRotulo: string | null;
  acaoUrl: string | null;
};

export type CentralNotificacoes = {
  itens: Notificacao[];
  naoLidas: number;
};

export class NotificacoesApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function ensureOk(response: Response) {
  if (response.ok) return;
  const body = await response.json().catch(() => null) as { detail?: string; message?: string } | null;
  throw new NotificacoesApiError(response.status, response.status === 401
    ? "Sua sessão expirou. Entre novamente para continuar."
    : body?.detail ?? body?.message ?? "Não foi possível carregar suas notificações.");
}

export async function listarNotificacoes(filtro: "TODAS" | "NAO_LIDAS", signal?: AbortSignal) {
  const response = await fetch(`/api/notificacoes?filtro=${filtro}`, {
    signal,
    credentials: "include",
    cache: "no-store",
    headers: { Accept: "application/json" },
  });
  await ensureOk(response);
  return await response.json() as CentralNotificacoes;
}

async function csrf() {
  const response = await fetch("/api/auth/csrf", {
    credentials: "include",
    cache: "no-store",
  });
  await ensureOk(response);
  return await response.json() as { headerName: string; token: string };
}

async function patch(path: string) {
  const token = await csrf();
  const response = await fetch(path, {
    method: "PATCH",
    credentials: "include",
    headers: { [token.headerName]: token.token },
  });
  await ensureOk(response);
}

export function marcarNotificacaoComoLida(id: number) {
  return patch(`/api/notificacoes/${encodeURIComponent(id)}/leitura`);
}

export function marcarTodasComoLidas() {
  return patch("/api/notificacoes/leitura");
}
