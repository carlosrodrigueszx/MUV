
import type { Vehicle } from "../types";
import { LuCarFront, LuPackage, LuPencil } from "react-icons/lu";

interface VehicleListProps {
  vehicles: Vehicle[];
  onEdit: (vehicle: Vehicle) => void;
}

export function VehicleList({ vehicles, onEdit }: VehicleListProps) {
  if (vehicles.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-[#cbd5d7] bg-white p-10 text-center">
        <LuCarFront className="mx-auto mb-3 text-[#17695b]" size={32} />
        <h2 className="font-semibold text-[#1d2d32]">
          Nenhum veículo cadastrado
        </h2>
        <p className="mt-2 text-sm text-[#637479]">
          Cadastre seu primeiro veículo para começar.
        </p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
      {vehicles.map(vehicle => (
        <article
          key={vehicle.id}
          className="rounded-xl border border-[#e1e8e9] bg-white p-5 shadow-sm"
        >
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-[#e5f2ee] text-[#17695b]">
                {vehicle.type === "passageiros" ? (
                  <LuCarFront size={23} />
                ) : (
                  <LuPackage size={23} />
                )}
              </div>

              <div>
                <h2 className="font-bold text-[#1d2d32]">
                  {vehicle.brand} {vehicle.model}
                </h2>
                <p className="text-xs text-[#637479]">
                  {vehicle.type === "passageiros"
                    ? "Transporte de passageiros"
                    : "Transporte de carga"}
                </p>
              </div>
            </div>

            <span className="rounded-full bg-[#e5f2ee] px-2.5 py-1 text-xs font-semibold text-[#17695b]">
              {vehicle.status === "ativo" ? "Ativo" : "Inativo"}
            </span>
          </div>

          <div className="mt-5 space-y-2 text-sm text-[#637479]">
            <p>
              <strong className="text-[#1d2d32]">Placa:</strong> {vehicle.plate}
            </p>
            <p>
              <strong className="text-[#1d2d32]">Ano:</strong> {vehicle.year}
            </p>
            <p>
              <strong className="text-[#1d2d32]">Cor:</strong> {vehicle.color}
            </p>
            <p>
              <strong className="text-[#1d2d32]">Capacidade:</strong>{" "}
              {vehicle.capacity} {vehicle.type === "passageiros" ? "pessoas" : "kg"}
            </p>
          </div>

          <button
            type="button"
            onClick={() => onEdit(vehicle)}
            className="mt-5 flex w-full items-center justify-center gap-2 rounded-lg border border-[#17695b] px-4 py-2.5 text-sm font-semibold text-[#17695b] transition-colors hover:bg-[#e5f2ee]"
          >
            <LuPencil size={16} />
            Editar veículo
          </button>
        </article>
      ))}
    </div>
  );
}