import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import bellIcon from "../assets/figma/bell.svg";
import chevronDownIcon from "../assets/figma/chevron-down.svg";
import routeIcon from "../assets/figma/route.svg";
import {
  searchProviders,
  type FreightModality,
  type ProviderSearchResult,
  type ProviderType,
} from "../features/provider-search/api";

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

function parseRegion(value: string) {
  const region = value.trim();
  if (!region) return {};

  const cityAndState = region.match(/^(.+?)\s*[,/]\s*([a-z]{2})$/i);
  if (cityAndState) {
    return { cidade: cityAndState[1].trim(), estado: cityAndState[2].toUpperCase() };
  }
  if (/^[a-z]{2}$/i.test(region)) return { estado: region.toUpperCase() };
  return { cidade: region };
}

interface ProviderCardProps {
  provider: ProviderSearchResult;
  onAction: (message: string) => void;
}

function ProviderCard({ provider, onAction }: ProviderCardProps) {
  const authorizedModalities = provider.modalidadesAutorizadas
    .map((modality) => modality === "PESSOA" ? "Pessoas" : "Encomendas")
    .join(" e ");

  return (
    <article
      id={`prestador-${provider.id}`}
      className="flex min-h-[319px] min-w-0 flex-col gap-4 rounded-[14px] border border-[#dfe7e8] bg-white p-6 max-md:min-h-0"
    >
      <div>
        <h2 className="w-full text-[19px] font-semibold leading-[1.45] text-[#1d2d32]">
          {provider.nome}
        </h2>
        <p className="text-[13px] text-[#637479]">
          {provider.tipo === "EMPRESA" ? "Empresa" : "Autônomo"}
        </p>
      </div>
      <div className="flex min-h-[75px] w-full flex-col justify-center gap-1 rounded-[14px] bg-[#e5f2ee] px-4 py-3 leading-[1.45]">
        <p className="text-[14px] font-semibold text-[#104e43]">
          Autorizado para {authorizedModalities}
        </p>
        <p className="text-[13px] font-normal text-[#1d2d32]">
          {provider.regioesAtendidas
            .map(({ cidade, estado }) => `${cidade} - ${estado}`)
            .join(", ")}
        </p>
      </div>
      <div className="flex w-full flex-col gap-3 text-[14px] leading-[1.45] text-[#1d2d32]">
        <p className="font-semibold">Veículos disponíveis</p>
        {provider.veiculosDisponiveis.map((vehicle) => (
          <div key={vehicle.id}>
            <p className="font-semibold">
              {vehicle.marca} {vehicle.modelo} ({vehicle.ano})
            </p>
            {vehicle.capacidadePassageiros > 0 && (
              <p>Até {vehicle.capacidadePassageiros} passageiros</p>
            )}
            {vehicle.capacidadeCargaKg > 0 && (
              <p>Até {vehicle.capacidadeCargaKg} kg de carga</p>
            )}
          </div>
        ))}
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
  const [service, setService] = useState<FreightModality | "">("");
  const [region, setRegion] = useState("");
  const [providerType, setProviderType] = useState<ProviderType | "">("");
  const [providers, setProviders] = useState<ProviderSearchResult[]>([]);
  const [hasSearched, setHasSearched] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState("");
  const activeSearch = useRef<AbortController | null>(null);

  useEffect(() => () => {
    activeSearch.current?.abort();
    activeSearch.current = null;
  }, []);

  async function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    activeSearch.current?.abort();
    const controller = new AbortController();
    activeSearch.current = controller;
    setLoading(true);
    setError("");
    setHasSearched(true);

    const filters = parseRegion(region);
    try {
      const results = await searchProviders({
        ...filters,
        ...(service && { modalidade: service }),
        ...(providerType && { tipo: providerType }),
      }, controller.signal);
      if (activeSearch.current === controller) setProviders(results);
    } catch (searchError) {
      if (
        activeSearch.current === controller &&
        !(searchError instanceof DOMException && searchError.name === "AbortError")
      ) {
        setError(
          searchError instanceof Error
            ? searchError.message
            : "Não foi possível consultar os prestadores.",
        );
      }
    } finally {
      if (activeSearch.current === controller) {
        activeSearch.current = null;
        setLoading(false);
      }
    }
  }

  const resultCount = providers.length;

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
              <label className="flex min-w-0 flex-col gap-2 text-[12px] font-semibold leading-[17px] text-[#637479]" htmlFor="service">
                Modalidade
                <select
                  id="service"
                  value={service}
                  onChange={(event) => {
                    const value = event.target.value;
                    if (value === "" || value === "PESSOA" || value === "ENCOMENDA") {
                      setService(value);
                    }
                  }}
                  className="h-[46px] w-full rounded-[8px] border border-[#dfe7e8] bg-[#f5f7f8] px-3 text-[14px] font-normal leading-5 text-[#1d2d32] outline-none transition focus:border-[#17695b] focus:ring-2 focus:ring-[#17695b]/15"
                >
                  <option value="">Todas</option>
                  <option value="PESSOA">Pessoas</option>
                  <option value="ENCOMENDA">Encomendas</option>
                </select>
              </label>
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
                      if (value === "" || value === "EMPRESA" || value === "AUTONOMO") {
                        setProviderType(value);
                      }
                    }}
                    className="h-[46px] w-full appearance-none rounded-[8px] border border-[#dfe7e8] bg-[#f5f7f8] px-3 pr-9 text-[14px] font-normal leading-5 text-[#1d2d32] outline-none transition focus:border-[#17695b] focus:ring-2 focus:ring-[#17695b]/15"
                  >
                    <option value="">Todos</option>
                    <option value="EMPRESA">Empresa</option>
                    <option value="AUTONOMO">Autônomo</option>
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
          {loading
            ? "Consultando prestadores..."
            : resultCount > 0
            ? `${resultCount} ${
                resultCount === 1
                  ? "prestador atende"
                  : "prestadores atendem"
              } sua rota`
            : "Prestadores disponíveis"}
        </h2>

        {loading ? (
          <section aria-live="polite" className="rounded-[14px] border border-[#dfe7e8] bg-white p-6">
            <p className="text-[14px] leading-[1.45] text-[#637479]">
              Consultando prestadores...
            </p>
          </section>
        ) : error ? (
          <section role="alert" className="rounded-[14px] border border-[#dfe7e8] bg-white p-6">
            <p className="text-[14px] leading-[1.45] text-[#1d2d32]">{error}</p>
          </section>
        ) : resultCount > 0 ? (
          <section
            aria-label="Prestadores que atendem sua rota"
            className="grid w-full grid-cols-2 items-start gap-5 max-md:grid-cols-1"
          >
            {providers.map((provider) => (
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
              {hasSearched
                ? "Nenhum prestador encontrado para os filtros informados."
                : "Informe os filtros desejados e pesquise para consultar os prestadores."}
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
