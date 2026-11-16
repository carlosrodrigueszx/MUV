
import { useCallback, useEffect, useState } from "react";
import type { Vehicle, NewVehiclePayload } from "../types";
import type { VehicleApi } from "../api/vehicleMockApi";

export function useVehicles(api: VehicleApi) {
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadVehicles = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const result = await api.listVehicles();
      setVehicles(result);
    } catch {
      setError("Não foi possível carregar os veículos.");
    } finally {
      setLoading(false);
    }
  }, [api]);

  useEffect(() => {
    void loadVehicles();
  }, [loadVehicles]);

  const createVehicle = useCallback(async (payload: NewVehiclePayload) => {
    await api.createVehicle(payload);
    await loadVehicles();
  }, [api, loadVehicles]);

  const updateVehicle = useCallback(async (
    id: string,
    payload: NewVehiclePayload,
  ) => {
    await api.updateVehicle(id, payload);
    await loadVehicles();
  }, [api, loadVehicles]);

  return {
    vehicles,
    loading,
    error,
    loadVehicles,
    createVehicle,
    updateVehicle,
  };
}