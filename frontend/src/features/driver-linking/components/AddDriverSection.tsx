import type { AccountSearchStatus, DriverAccount } from "../types";
import { DriverSummary } from "./DriverSummary";

interface AddDriverSectionProps {
  identifier: string;
  onIdentifierChange: (value: string) => void;
  onSearch: () => void;
  onNewDriver: () => void;
  status: AccountSearchStatus;
  account: DriverAccount | null;
  onLink: () => void;
  isLinking: boolean;
  linkError: string | null;
}

const outlineButton =
  "h-[42px] rounded-lg border border-[#dfe7e8] bg-white px-5 text-[13px] leading-[19px] font-semibold text-[#17695b] transition-colors hover:bg-[#f5f7f8] disabled:opacity-60";

export function AddDriverSection({
  identifier,
  onIdentifierChange,
  onSearch,
  onNewDriver,
  status,
  account,
  onLink,
  isLinking,
  linkError,
}: AddDriverSectionProps) {
  return (
    <section className="flex flex-col gap-4 rounded-[14px] border border-[#dfe7e8] bg-white p-6 shadow-[0_4px_16px_rgba(0,0,0,0.1)]">
      <h2 className="text-[19px] leading-7 font-semibold text-[#1d2d32]">
        Adicionar motorista à empresa
      </h2>

      <form
        className="flex flex-col gap-4"
        onSubmit={(event) => {
          event.preventDefault();
          onSearch();
        }}
      >
        <div className="flex flex-col gap-2">
          <label
            htmlFor="driver-identifier"
            className="text-[12px] leading-[17px] font-semibold text-[#637479]"
          >
            CPF ou e-mail da conta existente *
          </label>
          <input
            id="driver-identifier"
            type="text"
            value={identifier}
            onChange={(event) => onIdentifierChange(event.target.value)}
            className="h-[46px] w-full rounded-lg border border-[#dfe7e8] bg-[#f5f7f8] px-[13px] text-[14px] text-[#1d2d32] outline-none placeholder:text-[#637479] focus-visible:ring-2 focus-visible:ring-[#17695b]/40"
          />
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3 py-3">
          <button
            type="submit"
            disabled={status === "loading" || !identifier.trim()}
            className={outlineButton}
          >
            Buscar motorista
          </button>
          <div className="flex items-center gap-3">
            <span className="text-[13px] leading-[19px] text-[#637479]">
              Não tem cadastro? Cadastre na opção “Novo motorista”.
            </span>
            <button
              type="button"
              onClick={onNewDriver}
              className={outlineButton}
            >
              Novo motorista
            </button>
          </div>
        </div>
      </form>

      {status === "notFound" && (
        <p role="status" className="text-[13px] leading-[19px] text-[#637479]">
          Nenhuma conta encontrada para o CPF ou e-mail informado.
        </p>
      )}
      {status === "error" && (
        <p role="alert" className="text-[13px] leading-[19px] text-[#637479]">
          Não foi possível buscar a conta agora. Tente novamente.
        </p>
      )}

      {account && (
        <div className="flex flex-wrap items-center justify-between gap-4 rounded-[14px] border border-[#dfe7e8] bg-[#e5f2ee] p-4">
          <div className="flex flex-col gap-3">
            <h3 className="text-[16px] leading-6 font-semibold text-[#17695b]">
              Conta localizada
            </h3>
            <DriverSummary driver={account} size="sm" />
          </div>
          <div className="flex flex-col items-end gap-2">
            <button
              type="button"
              onClick={onLink}
              disabled={isLinking}
              className="h-[46px] rounded-lg bg-[#17695b] px-[18px] text-[13px] leading-[19px] font-semibold text-white transition-colors hover:bg-[#104e43] disabled:opacity-60"
            >
              Vincular Motorista
            </button>
            {linkError && (
              <p
                role="alert"
                className="text-[12px] leading-[17px] text-[#637479]"
              >
                {linkError}
              </p>
            )}
          </div>
        </div>
      )}
    </section>
  );
}
