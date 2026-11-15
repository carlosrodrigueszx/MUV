import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import bellIcon from "../assets/figma/bell.svg";
import chevronDownIcon from "../assets/figma/chevron-down.svg";
import routeIcon from "../assets/figma/route.svg";

type ProviderType = "Empresa" | "Autônomo";
type ProviderFilter = "Todos" | ProviderType;

interface Provider {
  id: string;
  name: string;
  type: ProviderType;
  regions: string;
  description: string;
  vehicle: string;
  capacity: string;
}

const providers: Provider[] = [];

interface SelectFieldProps {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
}

function SelectField({
  id,
  label,
  value,
  onChange,
  placeholder,
}: SelectFieldProps) {
  return (
    <label className="flex min-w-0 flex-col gap-2 text-[12px] font-semibold leading-[17px] text-[#637479]" htmlFor={id}>
      {label}
      <input
        id={id}
        type="text"
        value={value}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value)}
        className="h-[46px] w-full rounded-[8px] border border-[#dfe7e8] bg-[#f5f7f8] px-3 text-[14px] font-normal leading-5 text-[#1d2d32] outline-none transition placeholder:text-[#89979a] focus:border-[#17695b] focus:ring-2 focus:ring-[#17695b]/15"
      />
    </label>
  );
}

interface ProviderCardProps {
  provider: Provider;
  onAction: (message: string) => void;
}

function ProviderCard({ provider, onAction }: ProviderCardProps) {
  return (
    <article
      id={provider.id}
      className="flex min-h-[319px] min-w-0 flex-col gap-4 rounded-[14px] border border-[#dfe7e8] bg-white p-6 max-md:min-h-0"
    >
      <h2 className="w-full text-[19px] font-semibold leading-[1.45] text-[#1d2d32]">
        {provider.name}
      </h2>
      <div className="flex min-h-[75px] w-full flex-col justify-center gap-1 rounded-[14px] bg-[#e5f2ee] px-4 py-3 leading-[1.45]">
        <p className="text-[14px] font-semibold text-[#104e43]">
          Autorizado para encomendas
        </p>
        <p className="text-[13px] font-normal text-[#1d2d32]">
          {provider.regions}
        </p>
      </div>
      <p className="w-full text-[14px] font-normal leading-[1.45] text-[#1d2d32]">
        {provider.description}
      </p>
      <div className="w-full text-[14px] font-semibold leading-[1.45] text-[#1d2d32]">
        <p>{provider.vehicle}</p>
        <p>{provider.capacity}</p>
      </div>
      <div className="flex flex-wrap items-start gap-5">
        <button
          type="button"
          onClick={() =>
            onAction("O envio de solicitações ainda não está conectado.")
          }
          className="h-[42px] min-w-[194px] rounded-[8px] bg-[#17695b] px-[18px] text-[13px] font-semibold text-white transition-colors hover:bg-[#12584d] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#17695b]"
        >
          Solicitar a este prestador
        </button>
        <button
          type="button"
          onClick={() =>
            onAction("Os detalhes estarão disponíveis quando houver dados do prestador.")
          }
          className="h-[42px] min-w-[102px] rounded-[8px] border border-[#dfe7e8] bg-white px-[18px] text-[13px] font-semibold text-[#17695b] transition-colors hover:bg-[#f5f7f8] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#17695b]"
        >
          Ver dados
        </button>
      </div>
    </article>
  );
}

export default function ClientePrestadores() {
  const [service, setService] = useState("");
  const [region, setRegion] = useState("");
  const [providerType, setProviderType] = useState<ProviderFilter>("Todos");
  const [appliedProviderType, setAppliedProviderType] =
    useState<ProviderFilter>("Todos");
  const [feedback, setFeedback] = useState("");

  const visibleProviders = providers.filter(
    (provider) =>
      appliedProviderType === "Todos" ||
      provider.type === appliedProviderType,
  );

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAppliedProviderType(providerType);
    setFeedback("A busca será conectada à API quando ela estiver disponível.");
  }

  return (
    <div className="min-h-screen bg-[#f5f7f8] text-[#1d2d32] [font-family:Inter,'Noto_Sans',Arial,sans-serif]">
      <header className="sticky top-0 z-20 h-20 border-b border-[#dfe6e7] bg-white">
        <div className="mx-auto flex h-full max-w-[1440px] items-center justify-between px-12 max-md:px-4">
          <Link to="/" className="flex shrink-0 items-center gap-[9px]">
            <span className="flex size-8 items-center justify-center rounded-[10px] bg-[#17695b]">
              <img src={routeIcon} alt="" width={20} height={20} />
            </span>
            <span className="text-[25px] font-bold leading-none tracking-[-1px] text-[#104e43]">
              MUV
            </span>
          </Link>

          <nav
            aria-label="Navegação do passageiro"
            className="flex h-full shrink-0 items-center gap-9 max-md:hidden"
          >
            <Link
              to="/"
              className="flex h-full flex-col items-center justify-center"
            >
              <span className="flex h-[77px] items-center text-[14px] font-semibold leading-[1.45] text-[#637479]">
                Solicitar frete
              </span>
              <span className="h-[3px] w-full bg-[#17695b]" />
            </Link>
            <Link
              to="/cliente/prestadores"
              aria-current="page"
              className="flex h-full flex-col items-center justify-center"
            >
              <span className="flex h-[77px] items-center text-[14px] font-medium leading-[1.45] text-[#17695b]">
                Prestadores
              </span>
              <span className="h-[3px] w-full bg-transparent" />
            </Link>
            <a
              href="#meus-fretes"
              onClick={(event) => {
                event.preventDefault();
                setFeedback("A área de fretes ainda não está conectada.");
              }}
              className="flex h-full flex-col items-center justify-center"
            >
              <span className="flex h-[77px] items-center text-[14px] font-medium leading-[1.45] text-[#637479]">
                Meus fretes
              </span>
              <span className="h-[3px] w-full bg-transparent" />
            </a>
          </nav>

          <div className="flex shrink-0 items-center gap-5 max-sm:gap-3">
            <button
              type="button"
              onClick={() =>
                setFeedback("A troca de perfil ainda não está conectada.")
              }
              className="whitespace-nowrap text-[12px] font-medium leading-[1.45] text-[#637479] max-sm:hidden"
            >
              Trocar perfil
            </button>
            <span className="h-6 w-px bg-[#dfe6e7] max-sm:hidden" />
            <button
              type="button"
              aria-label="Notificações"
              onClick={() =>
                setFeedback("As notificações ainda não estão conectadas.")
              }
              className="flex size-5 items-center justify-center"
            >
              <img src={bellIcon} alt="" width={20} height={20} />
            </button>
            <button
              type="button"
              aria-label="Perfil de AO"
              onClick={() =>
                setFeedback("A troca de perfil ainda não está conectada.")
              }
              className="flex size-9 items-center justify-center rounded-full bg-[#e6f2ee] text-[12px] font-semibold leading-[1.45] text-[#17695b]"
            >
              AO
            </button>
            <button
              type="button"
              aria-label="Abrir opções do perfil"
              onClick={() =>
                setFeedback("A troca de perfil ainda não está conectada.")
              }
              className="flex size-4 items-center justify-center"
            >
              <img
                src={chevronDownIcon}
                alt=""
                width={16}
                height={16}
              />
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto flex w-full max-w-[1440px] flex-col gap-6 px-12 pb-[79px] pt-9 max-lg:px-8 max-md:gap-6 max-md:px-4 max-md:pb-12 max-md:pt-6">
        <section
          aria-label="Consultar prestadores"
          className="flex flex-col gap-6"
        >
          {/* <p className="h-4 w-full text-[11px] font-bold leading-4 text-[#17695b]">
            RF005&nbsp; / &nbsp;MUV
          </p> */}
          <div className="flex flex-col gap-6">
            <h1 className="text-[30px] font-bold leading-[1.45] text-[#1d2d32]">
              Encontre o prestador certo
            </h1>
            <p className="text-[14px] font-normal leading-5 text-[#637479]">
              Pesquise por modalidade e região atendida antes de direcionar sua
              solicitação.
            </p>
          </div>

          <section className="flex w-full flex-col gap-4 rounded-[14px] border border-[#dfe7e8] bg-white p-6">
            <h2 className="text-[19px] font-semibold leading-[1.45] text-[#1d2d32]">
              Como podemos ajudar?
            </h2>
            <form
              onSubmit={handleSearch}
              className="grid w-full grid-cols-[repeat(3,minmax(0,1fr))_99px] items-end gap-5 max-md:grid-cols-1"
            >
              <SelectField
                id="service"
                label="Modalidade"
                value={service}
                onChange={setService}
                placeholder="Informe a modalidade"
              />
              <SelectField
                id="region"
                label="Região de atendimento"
                value={region}
                onChange={setRegion}
                placeholder="Informe a região"
              />
              <label className="flex min-w-0 flex-col gap-2 text-[12px] font-semibold leading-[17px] text-[#637479]" htmlFor="provider-type">
                Tipo de prestador
                <span className="relative block w-full">
                  <select
                    id="provider-type"
                    value={providerType}
                    onChange={(event) => {
                      const value = event.target.value;
                      if (
                        value === "Todos" ||
                        value === "Empresa" ||
                        value === "Autônomo"
                      ) {
                        setProviderType(value);
                      }
                    }}
                    className="h-[46px] w-full appearance-none rounded-[8px] border border-[#dfe7e8] bg-[#f5f7f8] px-3 pr-9 text-[14px] font-normal leading-5 text-[#1d2d32] outline-none transition focus:border-[#17695b] focus:ring-2 focus:ring-[#17695b]/15"
                  >
                    <option value="Todos">Todos</option>
                    <option value="Empresa">Empresa</option>
                    <option value="Autônomo">Autônomo</option>
                  </select>
                  <span aria-hidden="true" className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-[10px] text-[#1d2d32]">
                    ▾
                  </span>
                </span>
              </label>
              <button
                type="submit"
                className="h-[46px] rounded-[8px] bg-[#17695b] px-[18px] text-[13px] font-semibold text-white transition-colors hover:bg-[#12584d] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#17695b] max-md:w-full"
              >
                Pesquisar
              </button>
            </form>
          </section>
        </section>

        <h2 className="text-[17px] font-semibold leading-[1.45] text-[#1d2d32]">
          {visibleProviders.length > 0
            ? `${visibleProviders.length} ${
                visibleProviders.length === 1
                  ? "prestador atende"
                  : "prestadores atendem"
              } sua rota`
            : "Prestadores disponíveis"}
        </h2>

        {visibleProviders.length > 0 ? (
          <section
            aria-label="Prestadores que atendem sua rota"
            className="grid w-full grid-cols-2 items-start gap-5 max-md:grid-cols-1"
          >
            {visibleProviders.map((provider) => (
              <ProviderCard
                key={provider.id}
                provider={provider}
                onAction={setFeedback}
              />
            ))}
          </section>
        ) : (
          <section
            aria-label="Prestadores disponíveis"
            className="rounded-[14px] border border-[#dfe7e8] bg-white p-6"
          >
            <p className="text-[14px] leading-[1.45] text-[#637479]">
              Os prestadores serão exibidos aqui quando a conexão com a API
              estiver disponível.
            </p>
          </section>
        )}

        <section className="flex min-h-[172px] w-full flex-col items-start gap-4 rounded-[14px] border border-[#dfe7e8] bg-white p-6">
          <h2 className="text-[19px] font-semibold leading-[1.45] text-[#1d2d32]">
            Prefere receber opções?
          </h2>
          <p className="text-[14px] font-normal leading-[1.45] text-[#1d2d32]">
            A opção de solicitação aberta estará disponível após a integração
            com a API.
          </p>
          <button
            type="button"
            onClick={() =>
              setFeedback("O envio de solicitações ainda não está conectado.")
            }
            className="h-[42px] rounded-[8px] border border-[#dfe7e8] bg-white px-[18px] text-[13px] font-semibold text-[#17695b] transition-colors hover:bg-[#f5f7f8] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#17695b]"
          >
            Criar solicitação aberta
          </button>
        </section>
      </main>

      {feedback && (
        <p
          role="status"
          aria-live="polite"
          className="fixed bottom-4 right-4 z-30 max-w-[calc(100vw-2rem)] rounded-lg border border-[#dfe7e8] bg-white px-4 py-3 text-sm text-[#1d2d32] shadow-lg"
        >
          {feedback}
        </p>
      )}
    </div>
  );
}
