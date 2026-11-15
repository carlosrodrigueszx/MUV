const base = "/api/auth";

async function ensureOk(response: Response, fallback: string) {
  if (response.ok) return;
  const error = await response.json().catch(() => null) as { detail?: string } | null;
  throw new Error(error?.detail ?? fallback);
}

export async function authPost(path: string, data?: object | URLSearchParams) {
  const csrfResponse = await fetch(`${base}/csrf`, { cache: "no-store" });
  await ensureOk(csrfResponse, "Não foi possível iniciar a operação. Tente novamente.");
  const { headerName, token } = await csrfResponse.json() as { headerName: string; token: string };
  const form = data instanceof URLSearchParams;
  const response = await fetch(`${base}/${path}`, {
    method: "POST",
    headers: {
      [headerName]: token,
      ...(data && { "Content-Type": form ? "application/x-www-form-urlencoded" : "application/json" }),
    },
    body: form ? data : data ? JSON.stringify(data) : undefined,
  });
  await ensureOk(response, path === "login" && response.status === 401
    ? "E-mail ou senha inválidos."
    : "Não foi possível concluir. Tente novamente.");
}

export type Session = { id: number; nome: string; email: string };

export async function getSession(signal: AbortSignal) {
  const response = await fetch(`${base}/sessao`, { signal, cache: "no-store" });
  if (response.status === 401) return null;
  await ensureOk(response, "Não foi possível verificar sua sessão.");
  return await response.json() as Session;
}
