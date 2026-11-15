import type { FreightHistoryFilters, FreightHistoryRecord } from "../types";

export interface FreightHistoryService {
  list(filters: FreightHistoryFilters, signal?: AbortSignal): Promise<FreightHistoryRecord[]>;
}

interface FreightHistoryServiceOptions {
  baseUrl: string;
  getUserId: () => string | null;
  getAuthToken?: () => string | null;
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
  getUserId,
  getAuthToken,
}: FreightHistoryServiceOptions): FreightHistoryService {
  const normalizedBaseUrl = baseUrl.replace(/\/$/, "");

  return {
    async list(filters, signal) {
      const userId = getUserId();
      if (!userId) {
        throw new FreightHistoryApiError(
          401,
          "A identificação da conta ainda não está disponível.",
        );
      }

      const query = new URLSearchParams({
        perfil: filters.profile.toUpperCase(),
        periodo: filters.month,
      });
      if (filters.modality !== "todas") query.set("modalidade", filters.modality);
      if (filters.status !== "todas") query.set("situacao", filters.status);

      const headers = new Headers({
        Accept: "application/json",
        "X-User-Id": userId,
      });
      const token = getAuthToken?.();
      if (token) headers.set("Authorization", `Bearer ${token}`);

      const response = await fetch(
        `${normalizedBaseUrl}/api/historico-fretes?${query.toString()}`,
        { headers, signal },
      );

      if (!response.ok) {
        throw new FreightHistoryApiError(
          response.status,
          response.status === 401 || response.status === 403
            ? "Sua conta não possui acesso a este histórico."
            : "Não foi possível consultar o histórico de fretes.",
        );
      }

      return (await response.json()) as FreightHistoryRecord[];
    },
  };
}
