import type { OrganizationDriver, RequestStatus } from "../types";
import { DriverSummary } from "./DriverSummary";

interface OrganizationDriversSectionProps {
  query: string;
  onQueryChange: (value: string) => void;
  onSearch: () => void;
  drivers: OrganizationDriver[];
  status: RequestStatus;
}

export function OrganizationDriversSection({
  query,
  onQueryChange,
  onSearch,
  drivers,
  status,
}: OrganizationDriversSectionProps) {
  return (
    <section className="flex flex-col gap-4 rounded-[14px] border border-[#dfe7e8] bg-white p-6 shadow-[0_10px_16px_rgba(0,0,0,0.1)]">
      <h2 className="text-[19px] leading-7 font-semibold text-[#1d2d32]">
        Motoristas da organização
      </h2>

      <form
        className="flex items-end gap-4"
        onSubmit={(event) => {
          event.preventDefault();
          onSearch();
        }}
      >
        <div className="flex flex-1 flex-col gap-2">
          <label
            htmlFor="team-query"
            className="text-[12px] leading-[17px] font-semibold text-[#637479]"
          >
            Consultar equipe
          </label>
          <input
            id="team-query"
            type="text"
            value={query}
            onChange={(event) => onQueryChange(event.target.value)}
            placeholder="Buscar por nome, CPF ou CNH"
            className="h-[46px] w-full rounded-lg bg-[#f5f7f8] px-[13px] border border-[#dfe7e8] text-[14px] text-[#1d2d32] outline-none placeholder:text-[#637479] focus-visible:ring-2 focus-visible:ring-[#17695b]/40"
          />
        </div>
        <button
          type="submit"
          disabled={status === "loading"}
          className="mb-px h-11 rounded-lg bg-[#17695b] px-[18px] text-[13px] leading-[19px] font-semibold text-white transition-colors hover:bg-[#104e43] disabled:opacity-60"
        >
          Consultar
        </button>
      </form>

      {status === "error" && (
        <p role="alert" className="text-[13px] leading-[19px] text-[#637479]">
          Não foi possível carregar os motoristas. Tente novamente.
        </p>
      )}
      {status === "ready" && drivers.length === 0 && (
        <p className="text-[13px] leading-[19px] text-[#637479]">
          Nenhum motorista encontrado.
        </p>
      )}

      {drivers.map((driver) => (
        <article
          key={driver.id}
          className="rounded-[14px] border border-[#dfe7e8] bg-white p-4"
        >
          <DriverSummary driver={driver} size="md" />
        </article>
      ))}

      <p className="text-[13px] leading-[19px] text-[#637479]">
        Somente motoristas vinculados e verificados poderão ser designados para
        os serviços da organização.
      </p>
    </section>
  );
}
