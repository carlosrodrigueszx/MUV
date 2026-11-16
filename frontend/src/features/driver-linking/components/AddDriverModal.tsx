import {
  useEffect,
  useId,
  useRef,
  useState,
  type ChangeEvent,
  type FormEvent,
  type ReactNode,
} from "react";
import { createPortal } from "react-dom";
import { LuUpload } from "react-icons/lu";
import type { NewDriverPayload, SelectOption } from "../types";

/** Opções padrão dos selects; podem ser substituídas por dados da API via props. */
const defaultGenderOptions: SelectOption[] = [
  { value: "masculino", label: "Masculino" },
  { value: "feminino", label: "Feminino" },
  { value: "outro", label: "Outro" },
];

const defaultCnhCategoryOptions: SelectOption[] = ["A", "B", "AB", "C", "AC", "D", "AD", "E", "AE"].map(
  (value) => ({ value, label: value }),
);

interface AddDriverModalProps {
  open: boolean;
  onClose: () => void;
  /** Deve rejeitar a Promise em caso de erro; o modal exibe a mensagem. */
  onSubmit: (payload: NewDriverPayload) => Promise<void>;
  genderOptions?: SelectOption[];
  cnhCategoryOptions?: SelectOption[];
}

const inputBox =
  "h-[46px] w-full rounded-xl border border-[#dfe7e8] bg-white px-3 text-[14px] leading-5 text-black outline-none placeholder:text-black/50 focus-visible:border-[#17695b] focus-visible:ring-2 focus-visible:ring-[#17695b]/30";

function Field({
  label,
  htmlFor,
  full = false,
  children,
}: {
  label: string;
  htmlFor: string;
  full?: boolean;
  children: ReactNode;
}) {
  return (
    <div className={`flex flex-col gap-2 px-3 ${full ? "col-span-2" : ""}`}>
      <label htmlFor={htmlFor} className="text-[14px] leading-[17px] font-medium text-black">
        {label}
      </label>
      {children}
    </div>
  );
}

function SelectBox({
  id,
  value,
  onChange,
  options,
}: {
  id: string;
  value: string;
  onChange: (value: string) => void;
  options: SelectOption[];
}) {
  return (
    <div className="relative">
      <select
        id={id}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className={`${inputBox} appearance-none pr-9`}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <svg
        aria-hidden="true"
        width="10"
        height="6"
        viewBox="0 0 10 6"
        fill="none"
        className="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2"
      >
        <path
          d="M1 1l4 4 4-4"
          stroke="currentColor"
          strokeWidth="1.5"
          strokeLinecap="round"
          strokeLinejoin="round"
          className="text-[#1d2d32]"
        />
      </svg>
    </div>
  );
}

function buildInitialForm(
  genderOptions: SelectOption[],
  cnhCategoryOptions: SelectOption[],
): NewDriverPayload {
  return {
    name: "",
    birthDate: "",
    gender: genderOptions[0]?.value ?? "",
    cpf: "",
    cnh: "",
    cnhCategory:
      cnhCategoryOptions.find((option) => option.value === "D")?.value ??
      cnhCategoryOptions[0]?.value ??
      "",
    phone: "",
    email: "",
    password: "",
    passwordConfirmation: "",
    photo: null,
  };
}

export function AddDriverModal(props: AddDriverModalProps) {
  // O conteúdo só é montado quando aberto: o formulário reinicia a cada abertura.
  return props.open ? <AddDriverModalContent {...props} /> : null;
}

function AddDriverModalContent({
  onClose,
  onSubmit,
  genderOptions = defaultGenderOptions,
  cnhCategoryOptions = defaultCnhCategoryOptions,
}: AddDriverModalProps) {
  const [form, setForm] = useState(() => buildInitialForm(genderOptions, cnhCategoryOptions));
  const [photoPreview, setPhotoPreview] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const uid = useId();
  const id = (name: string) => `${uid}-${name}`;
  const fileInputRef = useRef<HTMLInputElement>(null);
  const previewRef = useRef<string | null>(null);

  // Libera a URL do preview ao fechar.
  useEffect(
    () => () => {
      if (previewRef.current) URL.revokeObjectURL(previewRef.current);
    },
    [],
  );

  // Esc fecha e bloqueia o scroll da página enquanto aberto.
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !isSubmitting) onClose();
    };
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [isSubmitting, onClose]);

  const handlePhotoChange = (event: ChangeEvent<HTMLInputElement>) => {
    const photo = event.target.files?.[0] ?? null;
    if (previewRef.current) URL.revokeObjectURL(previewRef.current);
    previewRef.current = photo ? URL.createObjectURL(photo) : null;
    setPhotoPreview(previewRef.current);
    setForm((current) => ({ ...current, photo }));
  };

  const setField =
    (field: keyof NewDriverPayload) =>
    (event: ChangeEvent<HTMLInputElement>) =>
      setForm((current) => ({ ...current, [field]: event.target.value }));

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (form.password !== form.passwordConfirmation) {
      setError("As senhas não coincidem.");
      return;
    }
    setIsSubmitting(true);
    setError(null);
    try {
      await onSubmit(form);
      onClose();
    } catch (error) {
      setError(error instanceof Error ? error.message : "Não foi possível adicionar o motorista.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return createPortal(
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center overflow-y-auto bg-[#122d35]/65 p-4"
      style={{ fontFamily: "Inter, ui-sans-serif, system-ui, sans-serif" }}
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !isSubmitting) onClose();
      }}
    >
      <form
        role="dialog"
        aria-modal="true"
        aria-labelledby={id("title")}
        onSubmit={handleSubmit}
        className="my-auto flex w-[624px] max-w-full flex-col items-center gap-6 rounded-3xl border border-[#dfe7e8] bg-white py-[42px] shadow-[0_7px_10px_rgba(0,0,0,0.1)]"
      >
        <h2 id={id("title")} className="sr-only">
          Novo motorista
        </h2>

        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          className="flex h-[140px] w-[140px] shrink-0 flex-col items-center justify-center gap-3 overflow-hidden rounded-full border border-[#dfe7e8] bg-[#d9d9d9] text-[#6d6d6d] transition-colors hover:bg-[#d0d0d0]"
        >
          {photoPreview ? (
            <img src={photoPreview} alt="Foto do motorista" className="h-full w-full object-cover" />
          ) : (
            <>
              <LuUpload size={48} strokeWidth={1.5} className="text-[#4d4d4d]" />
              <span className="text-[14px] leading-[17px] font-semibold">Adicionar Foto</span>
            </>
          )}
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/png,image/jpeg,image/gif"
          className="hidden"
          onChange={handlePhotoChange}
        />

        <div className="grid w-full grid-cols-2 gap-y-3 px-8">
          <Field label="Nome Completo" htmlFor={id("name")} full>
            <input
              autoFocus
              id={id("name")}
              required
              value={form.name}
              onChange={setField("name")}
              placeholder="Nome completo do motorista"
              className={inputBox}
            />
          </Field>

          <Field label="Data de Nascimento" htmlFor={id("birth")}>
            <input
              id={id("birth")}
              required
              value={form.birthDate}
              onChange={setField("birthDate")}
              placeholder="Ex: 01/01/2000"
              inputMode="numeric"
              className={inputBox}
            />
          </Field>

          <Field label="Gênero" htmlFor={id("gender")}>
            <SelectBox
              id={id("gender")}
              value={form.gender}
              onChange={(gender) => setForm((current) => ({ ...current, gender }))}
              options={genderOptions}
            />
          </Field>

          <Field label="CPF" htmlFor={id("cpf")}>
            <input
              id={id("cpf")}
              required
              value={form.cpf}
              onChange={setField("cpf")}
              placeholder="CPF do Motorista"
              inputMode="numeric"
              className={inputBox}
            />
          </Field>

          <Field label="CNH" htmlFor={id("cnh")}>
            <input
              id={id("cnh")}
              required
              pattern="[0-9]{11}"
              maxLength={11}
              value={form.cnh}
              onChange={setField("cnh")}
              placeholder="CNH do Motorista"
              inputMode="numeric"
              className={inputBox}
            />
          </Field>

          <Field label="Categoria CNH" htmlFor={id("category")}>
            <SelectBox
              id={id("category")}
              value={form.cnhCategory}
              onChange={(cnhCategory) => setForm((current) => ({ ...current, cnhCategory }))}
              options={cnhCategoryOptions}
            />
          </Field>

          <Field label="Telefone" htmlFor={id("phone")}>
            <input
              id={id("phone")}
              type="tel"
              required
              value={form.phone}
              onChange={setField("phone")}
              placeholder="Telefone do Motorista"
              className={inputBox}
            />
          </Field>

          <Field label="E-mail" htmlFor={id("email")} full>
            <input
              id={id("email")}
              type="email"
              required
              value={form.email}
              onChange={setField("email")}
              placeholder="E-mail do Motorista"
              className={inputBox}
            />
          </Field>

          <Field label="Senha" htmlFor={id("password")}>
            <input
              id={id("password")}
              type="password"
              required
              minLength={8}
              maxLength={72}
              autoComplete="new-password"
              value={form.password}
              onChange={setField("password")}
              placeholder="Senha do Motorista"
              className={inputBox}
            />
          </Field>

          <Field label="Confirmar Senha" htmlFor={id("password-confirmation")}>
            <input
              id={id("password-confirmation")}
              type="password"
              required
              minLength={8}
              maxLength={72}
              autoComplete="new-password"
              value={form.passwordConfirmation}
              onChange={setField("passwordConfirmation")}
              placeholder="Confirmar Senha do Motorista"
              className={inputBox}
            />
          </Field>
        </div>

        {error && (
          <p role="alert" className="w-full px-11 text-[13px] leading-[19px] text-[#b42318]">
            {error}
          </p>
        )}

        <div className="flex w-full gap-2 px-11">
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="h-[49px] flex-1 rounded-xl border border-[#dfe7e8] bg-white text-[16px] leading-[23px] font-semibold text-[#17695b] transition-colors hover:bg-[#f5f7f8] disabled:opacity-60"
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="h-[49px] flex-1 rounded-xl bg-[#17695b] text-[16px] leading-[23px] font-semibold text-white transition-colors hover:bg-[#104e43] disabled:opacity-60"
          >
            Adicionar Motorista
          </button>
        </div>
      </form>
    </div>,
    document.body,
  );
}
