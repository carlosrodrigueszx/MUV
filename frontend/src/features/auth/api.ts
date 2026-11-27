const base = "/api/auth";
let sessionRevision = 0;
let cachedSession: { account: Session; expiresAt: number } | undefined;

export function readRecentSession(): Session | undefined {
  return cachedSession && cachedSession.expiresAt > Date.now()
    ? cachedSession.account : undefined;
}

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
  if (path === "login" || path === "logout" || path === "perfil/autonomo") {
    sessionRevision += 1;
    cachedSession = undefined;
  }
}

export type Session = { id: number; nome: string; email: string; podeAtivarAutonomo: boolean; prestador: boolean; motorista: boolean; passageiro: boolean; empresa: boolean };

export async function getSession(signal: AbortSignal): Promise<Session | null> {
  const revision = sessionRevision;
  const verificationSignal = AbortSignal.any([signal, AbortSignal.timeout(10_000)]);
  const response = await fetch(`${base}/sessao`, { signal: verificationSignal, cache: "no-store" });
  if (response.status !== 401) await ensureOk(response, "Não foi possível verificar sua sessão.");
  const account = response.status === 401 ? null : await response.json() as Session;
  verificationSignal.throwIfAborted();
  if (revision !== sessionRevision) return getSession(signal);
  cachedSession = account ? { account, expiresAt: Date.now() + 60_000 } : undefined;
  return account;
}
