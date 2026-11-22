export interface Achado {
  id: number;
  descricao: string;
  dataEncontrado: string;
  status: "EM_GUARDA" | "DEVOLVIDO" | string;
  veiculoId: number;
  veiculoMarca: string;
  veiculoModelo: string;
  veiculoPlaca: string;
  freteId: number | null;
  foto: string | null;
}

export interface OpcoesAchado {
  veiculos: Array<{ id: number; prestadorId: number; marca: string; modelo: string; placa: string }>;
  fretes: Array<{ id: number; prestadorId: number; status: string }>;
}

const base = "/api/achados-perdidos";

async function erro(resposta: Response): Promise<Error> {
  const corpo = await resposta.json().catch(() => null) as { detail?: string; message?: string } | null;
  const mensagens: Record<number, string> = {
    401: "Entre na sua conta para continuar.",
    403: "Esta área está disponível apenas para prestadores e motoristas vinculados.",
    404: "O registro ou veículo não foi encontrado.",
    409: "Este item já foi devolvido. Atualize a lista.",
  };
  return new Error(corpo?.detail ?? corpo?.message ?? mensagens[resposta.status]
    ?? "Não foi possível concluir a operação.");
}

async function consultar<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${base}${path}`, {
    credentials: "include", cache: "no-store", ...init,
  });
  if (!response.ok) throw await erro(response);
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

async function csrf() {
  const response = await fetch("/api/auth/csrf", { credentials: "include", cache: "no-store" });
  if (!response.ok) throw await erro(response);
  return response.json() as Promise<{ headerName: string; token: string }>;
}

export const listarAchados = () => consultar<Achado[]>("");
export const consultarOpcoesAchado = () => consultar<OpcoesAchado>("/opcoes");

export async function registrarAchado(payload: {
  descricao: string; dataEncontrado: string; veiculoId: number; freteId: number | null; foto: string;
}) {
  const token = await csrf();
  return consultar<{ id: number }>("", {
    method: "POST",
    headers: { "Content-Type": "application/json", [token.headerName]: token.token },
    body: JSON.stringify(payload),
  });
}

export async function confirmarDevolucao(id: number) {
  const token = await csrf();
  await consultar<void>(`/${encodeURIComponent(id)}/devolucao`, {
    method: "PATCH", headers: { [token.headerName]: token.token },
  });
}
