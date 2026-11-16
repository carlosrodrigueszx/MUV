import { useMemo, useState } from "react";
import { ProviderLayout } from "../components/layout/ProviderLayout";
import { createVehicleMockApi } from "../features/vehicles/api/vehicleMockApi";
import { useVehicles } from "../features/vehicles/hooks/useVehicles";
import { VehicleList } from "../features/vehicles/components/VehicleList";
import { VehicleForm } from "../features/vehicles/components/VehicleForm";
import type { Vehicle, NewVehiclePayload } from "../features/vehicles/types";

export default function Vehicles() {
  const api = useMemo(() => createVehicleMockApi(), []);
  const { vehicles, loading, error, createVehicle, updateVehicle } =
    useVehicles(api);

  const [formOpen, setFormOpen] = useState(false);
  const [selectedVehicle, setSelectedVehicle] = useState<Vehicle | null>(null);

  function handleNewVehicle() {
    setSelectedVehicle(null);
    setFormOpen(true);
  }

  function handleEditVehicle(vehicle: Vehicle) {
    setSelectedVehicle(vehicle);
    setFormOpen(true);
  }

  async function handleSubmit(payload: NewVehiclePayload) {
    if (selectedVehicle) {
      await updateVehicle(selectedVehicle.id, payload);
    } else {
      await createVehicle(payload);
    }
  }

  return (
    <ProviderLayout
      pageTitle="Gestão de veículos"
      activeNavKey="vehicles"
      organizationName="Transportadora MUV"
      organizationVerified
      isOnline
    >
      <div className="flex flex-col gap-7">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="mt-2 text-3xl font-bold text-[#1d2d32]">
              Gestão de veículos
            </h1>
            <p className="mt-2 text-sm text-[#637479]">
              Cadastre, consulte e atualize os veículos da sua organização.
            </p>
          </div>

          <button
            type="button"
            onClick={handleNewVehicle}
            className="rounded-lg bg-[#17695b] px-5 py-3 text-sm font-semibold text-white transition-colors hover:bg-[#125448]"
          >
            + Cadastrar veículo
          </button>
        </div>

        <div className="rounded-xl border border-[#e1e8e9] bg-white p-5">
          <p className="text-sm text-[#637479]">Veículos cadastrados</p>
          <p className="mt-1 text-3xl font-bold text-[#1d2d32]">
            {vehicles.length}
          </p>
        </div>

        {formOpen && (
          <VehicleForm
            vehicle={selectedVehicle}
            onSubmit={handleSubmit}
            onClose={() => setFormOpen(false)}
          />
        )}

        {loading ? (
          <p className="text-sm text-[#637479]">Carregando veículos...</p>
        ) : error ? (
          <p className="text-sm text-red-600">{error}</p>
        ) : (
          <VehicleList vehicles={vehicles} onEdit={handleEditVehicle} />
        )}
      </div>
    </ProviderLayout>
  );
}
