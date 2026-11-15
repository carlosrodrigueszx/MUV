import type {
  DriverAccount,
  NewDriverPayload,
  OrganizationDriver,
  ProviderProfile,
} from "../types";

/**
 * Contrato de acesso à API para o fluxo de vinculação de motoristas.
 * A UI e o hook dependem só desta interface: para ajustar a API, basta
 * alterar a implementação abaixo (ou injetar outra no `useDriverLinking`).
 */
export interface DriverLinkingApi {
  getProfile(signal?: AbortSignal): Promise<ProviderProfile>;
  /** Retorna `null` quando nenhuma conta é encontrada. */
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
}

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

/**
 * TODO(API): rotas provisórias — confirmar com o time de backend.
 * Centralizadas aqui para facilitar o ajuste.
 */
const ENDPOINTS = {
  profile: "/provider/profile",
  drivers: "/provider/drivers",
  lookup: "/provider/drivers/lookup",
  link: "/provider/drivers/link",
} as const;

interface HttpApiOptions {
  baseUrl: string;
  /** Opcional: devolve o token de autenticação atual. */
  getAuthToken?: () => string | null;
}

export function createDriverLinkingApi({
  baseUrl,
  getAuthToken,
}: HttpApiOptions): DriverLinkingApi {
  async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const headers = new Headers(init.headers);
    headers.set("Accept", "application/json");
    const token = getAuthToken?.();
    if (token) headers.set("Authorization", `Bearer ${token}`);

    const response = await fetch(`${baseUrl}${path}`, { ...init, headers });
    if (!response.ok) {
      throw new ApiError(
        response.status,
        response.statusText || "Falha na requisição",
      );
    }
    return (await response.json()) as T;
  }

  return {
    getProfile: (signal) =>
      request<ProviderProfile>(ENDPOINTS.profile, { signal }),

    async findAccount(identifier, signal) {
      try {
        return await request<DriverAccount>(
          `${ENDPOINTS.lookup}?identifier=${encodeURIComponent(identifier)}`,
          { signal },
        );
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) return null;
        throw error;
      }
    },

    linkDriver: (driverId) =>
      request<OrganizationDriver>(ENDPOINTS.link, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ driverId }),
      }),

    listOrganizationDrivers: (search, signal) => {
      const query = search ? `?search=${encodeURIComponent(search)}` : "";
      return request<OrganizationDriver[]>(`${ENDPOINTS.drivers}${query}`, {
        signal,
      });
    },

    createDriver(payload) {
      const { photo, passwordConfirmation, ...fields } = payload;
      void passwordConfirmation; // validada no front; não é enviada
      const body = new FormData();
      Object.entries(fields).forEach(([key, value]) => body.append(key, value));
      if (photo) body.append("photo", photo);
      return request<OrganizationDriver>(ENDPOINTS.drivers, {
        method: "POST",
        body,
      });
    },
  };
}
