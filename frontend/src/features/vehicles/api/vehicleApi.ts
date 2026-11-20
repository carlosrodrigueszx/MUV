import type { NewVehiclePayload, Vehicle } from "../types";

export interface VehicleApi {
  listVehicles(): Promise<Vehicle[]>;
  getVehicle(id: string): Promise<Vehicle | null>;
  createVehicle(payload: NewVehiclePayload): Promise<Vehicle>;
  updateVehicle(id: string, payload: NewVehiclePayload): Promise<Vehicle>;
}

interface VehicleResponse extends Omit<Vehicle, "id"> {
  id: number | string;
}

interface CsrfResponse {
  headerName: string;
  token: string;
}

const apiBase = (import.meta.env.VITE_API_BASE_URL ?? "")
  .replace(/\/+$/, "");

function vehiclePayload(payload: NewVehiclePayload) {
  return {
    type: payload.type,
    brand: payload.brand,
    model: payload.model,
    year: payload.year,
    plate: payload.plate,
    color: payload.color,
    capacity: payload.capacity,
    description: payload.description,
  };
}

function toVehicle(vehicle: VehicleResponse): Vehicle {
  return { ...vehicle, id: String(vehicle.id), photo: null };
}

async function responseError(response: Response): Promise<Error> {
  const body = await response.json().catch(() => null) as
    | { detail?: string; message?: string }
    | null;
  return new Error(body?.detail ?? body?.message ?? "Não foi possível concluir a operação.");
}

async function csrf(): Promise<CsrfResponse> {
  const response = await fetch(`${apiBase}/api/auth/csrf`, {
    credentials: "include",
  });
  if (!response.ok) throw await responseError(response);
  return response.json() as Promise<CsrfResponse>;
}

export function createVehicleApi(): VehicleApi {
  return {
    async listVehicles() {
      const response = await fetch(`${apiBase}/api/veiculos`, {
        credentials: "include",
      });
      if (!response.ok) throw await responseError(response);
      const vehicles = await response.json() as VehicleResponse[];
      return vehicles.map(toVehicle);
    },

    async getVehicle(id) {
      const response = await fetch(`${apiBase}/api/veiculos/${encodeURIComponent(id)}`, {
        credentials: "include",
      });
      if (response.status === 404) return null;
      if (!response.ok) throw await responseError(response);
      return toVehicle(await response.json() as VehicleResponse);
    },

    async createVehicle(payload) {
      const token = await csrf();
      const response = await fetch(`${apiBase}/api/veiculos`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json", [token.headerName]: token.token },
        body: JSON.stringify(vehiclePayload(payload)),
      });
      if (!response.ok) throw await responseError(response);
      return toVehicle(await response.json() as VehicleResponse);
    },

    async updateVehicle(id, payload) {
      const token = await csrf();
      const response = await fetch(`${apiBase}/api/veiculos/${encodeURIComponent(id)}`, {
        method: "PUT",
        credentials: "include",
        headers: { "Content-Type": "application/json", [token.headerName]: token.token },
        body: JSON.stringify(vehiclePayload(payload)),
      });
      if (!response.ok) throw await responseError(response);
      return toVehicle(await response.json() as VehicleResponse);
    },
  };
}
