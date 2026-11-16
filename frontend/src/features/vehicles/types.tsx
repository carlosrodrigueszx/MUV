
export type VehicleType = "passageiros" | "carga";

export interface Vehicle {
  id: string;
  type: VehicleType;
  brand: string;
  model: string;
  year: number;
  plate: string;
  color: string;
  capacity: number;
  description: string;
  status: "ativo" | "inativo";
  photo?: string | null;
}

export interface NewVehiclePayload {
  type: VehicleType;
  brand: string;
  model: string;
  year: number;
  plate: string;
  color: string;
  capacity: number;
  description: string;
  photo: File | null;
}