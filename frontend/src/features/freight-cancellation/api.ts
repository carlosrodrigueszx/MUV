export type CancellationDetails = {
  frete: {
    id: number;
    status: string;
    clienteNome: string;
    prestadorNome: string;
    modalidade: string;
    origem: string;
    destino: string;
    dataHoraPretendida: string;
  };
  autorNome: string;
  autorPapel: string;
  registro: { autor: string; dataHora: string; justificativa: string } | null;
};

export class CancellationError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function ensureOk(response: Response) {
  if (response.ok) return;
  const error = await response.json().catch(() => null) as { detail?: string } | null;
  throw new CancellationError(response.status, response.status === 401
    ? "Sua sessão expirou. Entre novamente para continuar."
    : error?.detail ?? "Não foi possível concluir a operação. Tente novamente.");
}

export async function getCancellation(id: string, signal?: AbortSignal): Promise<CancellationDetails> {
  const response = await fetch(`/api/fretes/${encodeURIComponent(id)}/cancelamento`, { signal, cache: "no-store" });
  await ensureOk(response);
  return response.json() as Promise<CancellationDetails>;
}

export async function cancelFreight(id: string, justificativa: string) {
  const csrfResponse = await fetch("/api/auth/csrf", { cache: "no-store" });
  await ensureOk(csrfResponse);
  const csrf = await csrfResponse.json() as { headerName: string; token: string };
  const response = await fetch(`/api/fretes/${encodeURIComponent(id)}/cancelamento`, {
    method: "POST",
    headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
    body: JSON.stringify({ justificativa }),
  });
  await ensureOk(response);
}
