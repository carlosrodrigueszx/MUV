/**
 * Tipos do fluxo "Vincular e verificar motorista".
 * Os formatos abaixo são o contrato esperado pela UI; quando a API estiver
 * pronta, ajuste apenas o mapeamento em `api/driverLinkingApi.ts`.
 */

export interface DriverAccount {
  id: string;
  name: string;
  cpf: string;
  cnh: string;
  cnhCategory: string;
  phone: string;
  email: string;
  gender: string;
  avatarUrl?: string | null;
}

/** Motorista já vinculado à organização. */
export type OrganizationDriver = DriverAccount;

/** Dados do prestador (empresa) logado, usados no layout e no subtítulo. */
export interface ProviderProfile {
  organizationName: string;
  verified: boolean;
  avatarUrl?: string | null;
  isOnline: boolean;
  pendingRequestsCount: number;
}

export interface SelectOption {
  value: string;
  label: string;
}

/** Payload do modal "Novo motorista". */
export interface NewDriverPayload {
  name: string;
  birthDate: string;
  gender: string;
  cpf: string;
  cnh: string;
  cnhCategory: string;
  phone: string;
  email: string;
  password: string;
  passwordConfirmation: string;
  photo: File | null;
}

export type AccountSearchStatus = "idle" | "loading" | "found" | "notFound" | "error";
export type RequestStatus = "idle" | "loading" | "ready" | "error";
