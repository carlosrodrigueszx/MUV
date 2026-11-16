export type VerificationStatus = "PENDENTE" | "VERIFICADO" | "REJEITADO";

export interface DriverAccount {
  id: string;
  name: string;
  cnh: string;
  cnhCategory: string;
  phone: string;
  cpf?: string | null;
  birthDate?: string | null;
  gender?: string | null;
  email?: string | null;
  avatarUrl?: string | null;
  statusVerificacao?: VerificationStatus;
  dataVerificacao?: string | null;
}
export type OrganizationDriver = DriverAccount;
export interface ProviderProfile {
  id: number;
  organizationName: string;
}
export interface SelectOption { value: string; label: string }
export interface NewDriverPayload {
  birthDate: string;
  gender: string;
  cpf: string;
  email: string;
  password: string;
  passwordConfirmation: string;
  photo: File | null;
  name: string;
  cnh: string;
  cnhCategory: string;
  phone: string;
}
export type AccountSearchStatus = "idle" | "loading" | "found" | "notFound" | "error";
export type RequestStatus = "idle" | "loading" | "ready" | "error";
