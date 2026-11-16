
import { useEffect, useState, type FormEvent } from "react";
import type { Vehicle, NewVehiclePayload } from "../types";

interface VehicleFormProps {
  vehicle: Vehicle | null;
  onSubmit: (payload: NewVehiclePayload) => Promise<void>;
  onClose: () => void;
}

const emptyForm: NewVehiclePayload = {
  type: "passageiros",
  brand: "",
  model: "",
  year: new Date().getFullYear(),
  plate: "",
  color: "",
  capacity: 1,
  description: "",
  photo: null,
};

export function VehicleForm({
  vehicle,
  onSubmit,
  onClose,
}: VehicleFormProps) {
  const [form, setForm] = useState<NewVehiclePayload>(emptyForm);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (vehicle) {
      setForm({
        type: vehicle.type,
        brand: vehicle.brand,
        model: vehicle.model,
        year: vehicle.year,
        plate: vehicle.plate,
        color: vehicle.color,
        capacity: vehicle.capacity,
        description: vehicle.description,
        photo: null,
      });
    } else {
      setForm({ ...emptyForm });
    }

    setError("");
  }, [vehicle]);

  function handleChange(
    event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>,
  ) {
    const { name, value } = event.target;

    setForm(current => ({
      ...current,
      [name]: name === "year" || name === "capacity" ? Number(value) : value,
    }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    if (form.capacity <= 0 || form.year <= 0) {
      setError("Informe um ano e uma capacidade válidos.");
      return;
    }

    try {
      setSaving(true);
      await onSubmit({
        ...form,
        plate: form.plate.trim().toUpperCase(),
      });
      onClose();
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Não foi possível salvar o veículo.",
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="space-y-5 rounded-xl border border-[#e1e8e9] bg-white p-6 shadow-sm"
    >
      <div>
        <h2 className="text-xl font-bold text-[#1d2d32]">
          {vehicle ? "Editar veículo" : "Cadastrar veículo"}
        </h2>
        <p className="mt-1 text-sm text-[#637479]">
          Preencha as informações do veículo.
        </p>
      </div>

      <div>
        <label className="mb-2 block text-sm font-semibold">Tipo de veículo</label>
        <select
          name="type"
          value={form.type}
          onChange={handleChange}
          className="w-full rounded-lg border border-[#cbd5d7] p-3 text-sm"
        >
          <option value="passageiros">Transporte de passageiros</option>
          <option value="carga">Transporte de carga</option>
        </select>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <label className="mb-2 block text-sm font-semibold">Marca</label>
          <input
            name="brand"
            value={form.brand}
            onChange={handleChange}
            required
            placeholder="Ex.: Mercedes-Benz"
            className="w-full rounded-lg border border-[#cbd5d7] p-3 text-sm"
          />
        </div>

        <div>
          <label className="mb-2 block text-sm font-semibold">Modelo</label>
          <input
            name="model"
            value={form.model}
            onChange={handleChange}
            required
            placeholder="Ex.: Sprinter"
            className="w-full rounded-lg border border-[#cbd5d7] p-3 text-sm"
          />
        </div>

        <div>
          <label className="mb-2 block text-sm font-semibold">Ano</label>
          <input
            name="year"
            type="number"
            min="1900"
            max={new Date().getFullYear() + 1}
            value={form.year}
            onChange={handleChange}
            required
            className="w-full rounded-lg border border-[#cbd5d7] p-3 text-sm"
          />
        </div>

        <div>
          <label className="mb-2 block text-sm font-semibold">Placa</label>
          <input
            name="plate"
            value={form.plate}
            onChange={handleChange}
            required
            maxLength={7}
            placeholder="ABC1D23"
            className="w-full rounded-lg border border-[#cbd5d7] p-3 text-sm uppercase"
          />
        </div>

        <div>
          <label className="mb-2 block text-sm font-semibold">Cor</label>
          <input
            name="color"
            value={form.color}
            onChange={handleChange}
            required
            placeholder="Ex.: Branco"
            className="w-full rounded-lg border border-[#cbd5d7] p-3 text-sm"
          />
        </div>

        <div>
          <label className="mb-2 block text-sm font-semibold">
            Capacidade ({form.type === "passageiros" ? "pessoas" : "kg"})
          </label>
          <input
            name="capacity"
            type="number"
            min="1"
            value={form.capacity}
            onChange={handleChange}
            required
            className="w-full rounded-lg border border-[#cbd5d7] p-3 text-sm"
          />
        </div>
      </div>

      <div>
        <label className="mb-2 block text-sm font-semibold">
          Características adicionais
        </label>
        <textarea
          name="description"
          value={form.description}
          onChange={handleChange}
          rows={3}
          placeholder="Descreva outras características do veículo..."
          className="w-full rounded-lg border border-[#cbd5d7] p-3 text-sm"
        />
      </div>

      {error && (
        <p className="text-sm text-red-600">{error}</p>
      )}

      <div className="flex flex-wrap justify-end gap-3">
        <button
          type="button"
          onClick={onClose}
          disabled={saving}
          className="rounded-lg border border-[#cbd5d7] px-5 py-2.5 text-sm font-semibold"
        >
          Cancelar
        </button>

        <button
          type="submit"
          disabled={saving}
          className="rounded-lg bg-[#17695b] px-5 py-2.5 text-sm font-semibold text-white hover:bg-[#125448] disabled:opacity-50"
        >
          {saving ? "Salvando..." : vehicle ? "Salvar alterações" : "Cadastrar veículo"}
        </button>
      </div>
    </form>
  );
}