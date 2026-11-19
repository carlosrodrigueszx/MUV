import type {
  DriverAccount,
  NewDriverPayload,
  OrganizationDriver,
  ProviderProfile,
  VerificationStatus,
} from "../types";

export interface DriverLinkingApi {
  getProfile(signal?: AbortSignal): Promise<ProviderProfile>;
  findAccount(
    identifier: string,
    signal?: AbortSignal,
  ): Promise<DriverAccount | null>;
  linkDriver(driverId: string): Promise<OrganizationDriver>;
  listOrganizationDrivers(
    search?: string,
    signal?: AbortSignal,
  ): Promise<OrganizationDriver[]>;
  createDriver(payload: NewDriverPayload): Promise<OrganizationDriver>;
  verifyDriver(
    driverId: string,
    status: VerificationStatus,
  ): Promise<OrganizationDriver>;
}
export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}
interface MotoristaResponse {
  id: number;
  nome: string;
  telefone: string;
  cnh: string;
  categoriaCnh: string;
  cpf?: string | null;
  nascimento?: string | null;
  genero?: string | null;
  email?: string | null;
  avatarUrl?: string | null;
  statusVerificacao?: VerificationStatus;
  dataVerificacao?: string | null;
}
function mapDriver(data: MotoristaResponse): DriverAccount {
  return {
    id: String(data.id),
    name: data.nome,
    phone: data.telefone,
    cnh: data.cnh,
    cpf: data.cpf,
    birthDate: data.nascimento,
    gender: data.genero,
    email: data.email,
    avatarUrl: data.avatarUrl,
    cnhCategory: data.categoriaCnh,
    statusVerificacao: data.statusVerificacao,
    dataVerificacao: data.dataVerificacao,
  };
}
export function createDriverLinkingApi({
  baseUrl,
}: {
  baseUrl: string;
}): DriverLinkingApi {
  const base = baseUrl.replace(/\/$/, "");
  async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const headers = new Headers(init.headers);
    headers.set("Accept", "application/json");
    if (init.method && init.method !== "GET") {
      const csrf = await request<{ headerName: string; token: string }>(
        "/api/auth/csrf",
      );
      headers.set(csrf.headerName, csrf.token);
      headers.set("Content-Type", "application/json");
    }
    const response = await fetch(`${base}${path}`, {
      ...init,
      headers,
      credentials: "include",
      cache: "no-store",
    });
    if (!response.ok) {
      const error = (await response.json().catch(() => null)) as {
        detail?: string;
      } | null;
      throw new ApiError(
        response.status,
        error?.detail ??
          (response.status === 401
            ? "Entre na sua conta de gestor para continuar."
            : "Não foi possível concluir a operação."),
      );
    }
    return (await response.json()) as T;
  }
  async function getProfile(signal?: AbortSignal): Promise<ProviderProfile> {
    const data = await request<{ id: number; nomeFantasia: string }>(
      "/api/prestadores/motoristas/perfil",
      { signal },
    );
    return { id: data.id, organizationName: data.nomeFantasia };
  }
  async function driversPath(signal?: AbortSignal) {
    const profile = await getProfile(signal);
    return `/api/prestadores/${profile.id}/motoristas`;
  }
  return {
    getProfile,
    async findAccount(identifier, signal) {
      const path = await driversPath(signal);
      try {
        return mapDriver(
          await request<MotoristaResponse>(
            `${path}/busca?identifier=${encodeURIComponent(identifier.trim())}`,
            { signal },
          ),
        );
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) return null;
        throw error;
      }
    },
    async listOrganizationDrivers(search, signal) {
      const path = await driversPath(signal);
      const data = await request<MotoristaResponse[]>(
        `${path}?search=${encodeURIComponent(search ?? "")}`,
        { signal },
      );
      return data.map(mapDriver);
    },
    async createDriver(payload) {
      const path = await driversPath();
      return mapDriver(
        await request<MotoristaResponse>(path, {
          method: "POST",
          body: JSON.stringify({
            nome: payload.name.trim(),
            telefone: payload.phone.trim(),
            cnh: payload.cnh.trim(),
            categoriaCnh: payload.cnhCategory,
            cpf: payload.cpf,
            nascimento: toIsoDate(payload.birthDate),
            genero: payload.gender,
            email: payload.email,
            senha: payload.password,
            confirmarSenha: payload.passwordConfirmation,
            foto: payload.photo ? await readPhoto(payload.photo) : null,
          }),
        }),
      );
    },
    async linkDriver(driverId) {
      const path = await driversPath();
      return mapDriver(
        await request<MotoristaResponse>(
          `${path}/${encodeURIComponent(driverId)}`,
          { method: "POST" },
        ),
      );
    },
    async verifyDriver(driverId, status) {
      const path = await driversPath();
      return mapDriver(
        await request<MotoristaResponse>(
          `${path}/${encodeURIComponent(driverId)}/verificacao`,
          {
            method: "PATCH",
            body: JSON.stringify({ statusVerificacao: status }),
          },
        ),
      );
    },
  };
}

function toIsoDate(date: string): string {
  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(date);
  return match ? `${match[3]}-${match[2]}-${match[1]}` : date;
}
function readPhoto(file: File): Promise<string> {
  if (file.size > 1048576)
    return Promise.reject(new Error("A foto deve ter no máximo 1 MB."));
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(new Error("Não foi possível ler a foto."));
    reader.readAsDataURL(file);
  });
}
