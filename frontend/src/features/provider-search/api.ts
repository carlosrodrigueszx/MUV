export type FreightModality = "PESSOA" | "ENCOMENDA";
export type ProviderType = "EMPRESA" | "AUTONOMO";

export interface Municipality {
  codigo: number;
  cidade: string;
  estado: string;
}

export interface ProviderSearchFilters {
  modalidade?: FreightModality;
  municipioCodigo?: number;
  cidade?: string;
  estado?: string;
  tipo?: ProviderType;
  quantidadePassageiros?: number;
}

export interface ProviderSearchResult {
  id: number;
  nome: string;
  tipo: ProviderType;
  modalidadesAutorizadas: FreightModality[];
  regioesAtendidas: Array<{ cidade: string; estado: string }>;
  cobertura?: { nivel: "DIRETA" | "PROXIMA"; municipio: Municipality };
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

export async function loadMunicipalities(signal: AbortSignal): Promise<Municipality[]> {
  const response = await fetch("/api/prestadores/consulta/municipios", { signal });
  if (!response.ok) throw new Error("Não foi possível carregar os municípios.");
  return await response.json() as Municipality[];
}

export async function searchProviders(
  filters: ProviderSearchFilters,
  signal: AbortSignal,
): Promise<ProviderSearchResult[]> {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(filters)) {
    if (value !== undefined && value !== "") params.set(key, String(value));
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
