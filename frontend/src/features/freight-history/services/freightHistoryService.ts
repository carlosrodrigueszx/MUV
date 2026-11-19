import type { AccountProfile, FreightHistoryFilters, FreightHistoryRecord } from "../types";

export interface FreightHistoryService {
  profiles(signal?: AbortSignal): Promise<AccountProfile[]>;
  list(filters: FreightHistoryFilters, signal?: AbortSignal): Promise<FreightHistoryRecord[]>;
}

interface FreightHistoryServiceOptions {
  baseUrl: string;
}

export class FreightHistoryApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "FreightHistoryApiError";
    this.status = status;
  }
}

export function createFreightHistoryService({
  baseUrl,
}: FreightHistoryServiceOptions): FreightHistoryService {
  const normalizedBaseUrl = baseUrl.replace(/\/$/, "");

  async function request(path: string, signal?: AbortSignal) {
    const response = await fetch(`${normalizedBaseUrl}${path}`, {
      headers: { Accept: "application/json" },
      credentials: "include",
      signal,
    });

    if (!response.ok) {
      throw new FreightHistoryApiError(
        response.status,
        response.status === 401 || response.status === 403
          ? "Sua conta não possui acesso a este histórico."
          : "Não foi possível consultar o histórico de fretes.",
      );
    }
    return response;
  }

  return {
    async profiles(signal) {
      const response = await request("/api/historico-fretes/perfis", signal);
      const profiles = (await response.json()) as string[];
      return profiles
        .map((profile) => profile.toLowerCase())
        .filter((profile): profile is AccountProfile =>
          profile === "cliente" || profile === "prestador");
    },

    async list(filters, signal) {
      const query = new URLSearchParams({
        perfil: filters.profile.toUpperCase(),
        periodo: filters.month,
      });
      if (filters.modality !== "todas") query.set("modalidade", filters.modality);
      if (filters.status !== "todas") query.set("situacao", filters.status);

      const response = await request(
        `/api/historico-fretes?${query.toString()}`,
        signal,
      );

      return (await response.json()) as FreightHistoryRecord[];
    },
  };
}
