
import type { NewVehiclePayload, Vehicle } from "../types";

export interface VehicleApi {
  listVehicles(): Promise<Vehicle[]>;
  getVehicle(id: string): Promise<Vehicle | null>;
  createVehicle(payload: NewVehiclePayload): Promise<Vehicle>;
  updateVehicle(id: string, payload: NewVehiclePayload): Promise<Vehicle>;
}

let vehicles: Vehicle[] = [
  {
    id: "1",
    type: "passageiros",
    brand: "Mercedes-Benz",
    model: "Sprinter",
    year: 2022,
    plate: "ABC1D23",
    color: "Branca",
    capacity: 16,
    description: "Van para transporte de passageiros.",
    status: "ativo",
    photo: null,
  },
  {
    id: "2",
    type: "carga",
    brand: "Volkswagen",
    model: "Delivery",
    year: 2021,
    plate: "DEF4G56",
    color: "Prata",
    capacity: 3500,
    description: "Caminhão para transporte de cargas.",
    status: "ativo",
    photo: null,
  },
];

function wait(): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, 300));
}

export function createVehicleMockApi(): VehicleApi {
  return {
    async listVehicles() {
      await wait();
      return vehicles.map(vehicle => ({ ...vehicle }));
    },

    async getVehicle(id) {
      await wait();
      const vehicle = vehicles.find(item => item.id === id);
      return vehicle ? { ...vehicle } : null;
    },

    async createVehicle(payload) {
      await wait();

      const plate = payload.plate.trim().toUpperCase();

      if (vehicles.some(vehicle => vehicle.plate === plate)) {
        throw new Error("Já existe um veículo cadastrado com essa placa.");
      }

      const vehicle: Vehicle = {
        ...payload,
        id: String(Date.now()),
        plate,
        status: "ativo",
        photo: null,
      };

      vehicles = [...vehicles, vehicle];

      return { ...vehicle };
    },

    async updateVehicle(id, payload) {
      await wait();

      const exists = vehicles.some(vehicle => vehicle.id === id);

      if (!exists) {
        throw new Error("Veículo não encontrado.");
      }

      const plate = payload.plate.trim().toUpperCase();

      const duplicate = vehicles.some(
        vehicle => vehicle.id !== id && vehicle.plate === plate,
      );

      if (duplicate) {
        throw new Error("Já existe outro veículo cadastrado com essa placa.");
      }

      const updatedVehicle: Vehicle = {
        ...payload,
        id,
        plate,
        status: vehicles.find(vehicle => vehicle.id === id)!.status,
        photo: null,
      };

      vehicles = vehicles.map(vehicle =>
        vehicle.id === id ? updatedVehicle : vehicle,
      );

      return { ...updatedVehicle };
    },
  };
}