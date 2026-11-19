import { Avatar } from "../../../components/ui/Avatar";
import type { DriverAccount } from "../types";

interface DriverSummaryProps {
  driver: DriverAccount;
  /** `sm`: cartão "Conta localizada"; `md`: lista da organização. */
  size?: "sm" | "md";
}

function Info({ label, value }: { label: string; value: string | null | undefined }) {
  if (!value) return null;
  return (
    <span className="whitespace-nowrap">
      <strong className="font-semibold">{label}:</strong> {value}
    </span>
  );
}

/** Foto + nome + dados do motorista, em 3 colunas (como no design). */
export function DriverSummary({ driver, size = "md" }: DriverSummaryProps) {
  const isSmall = size === "sm";
  return (
    <div className="flex min-h-[72px] items-center gap-3">
      <Avatar src={driver.avatarUrl} alt="" size={64} />
      <div className="min-w-0">
        <p
          className={`font-semibold text-[#1d2d32] ${
            isSmall ? "text-[17px] leading-6" : "text-[20px] leading-7"
          }`}
        >
          {driver.name}
        </p>
        <div
          className={`mt-0.5 grid w-max max-w-full grid-cols-[auto_auto_auto] gap-x-4 gap-y-0.5 text-[#1d2d32] ${
            isSmall ? "text-[12px] leading-[17px]" : "text-[14px] leading-5"
          }`}
        >
          <Info label="CPF" value={driver.cpf} />
          <Info label="CNH" value={driver.cnh} />
          <Info label="Categoria" value={driver.cnhCategory} />
          <Info label="Telefone" value={driver.phone} />
          <Info label="E-mail" value={driver.email} />
          <Info label="Gênero" value={driver.gender} />
        </div>
      </div>
    </div>
  );
}
