export type FreightModality = "PESSOA" | "ENCOMENDA";
export type ProviderType = "EMPRESA" | "AUTONOMO";

export interface ProviderSearchFilters {
  modalidade?: FreightModality;
  cidade?: string;
  estado?: string;
  tipo?: ProviderType;
}

export interface ProviderSearchResult {
  id: number;
  nome: string;
  tipo: ProviderType;
  modalidadesAutorizadas: FreightModality[];
  regioesAtendidas: Array<{ cidade: string; estado: string }>;
  veiculosDisponiveis: Array<{
    id: number;
    marca: string;
    modelo: string;
    ano: number;
    modalidade: FreightModality | "AMBOS";
    capacidadePassageiros: number;
    capacidadeCargaKg: number;
  }>;
}

export async function searchProviders(
  filters: ProviderSearchFilters,
  signal: AbortSignal,
): Promise<ProviderSearchResult[]> {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(filters)) {
    if (value) params.set(key, value);
  }

  const response = await fetch(`/api/prestadores/consulta?${params}`, {
    signal,
    headers: { Accept: "application/json" },
  });
  if (!response.ok) {
    const error = await response.json().catch(() => null) as
      | { detail?: string; message?: string }
      | null;
    throw new Error(
      error?.detail ?? error?.message ?? "Não foi possível consultar os prestadores.",
    );
  }

  return await response.json() as ProviderSearchResult[];
}
