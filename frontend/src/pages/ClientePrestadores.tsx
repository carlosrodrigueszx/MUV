import { AccountNavbar } from "../features/freight-history/components/AccountNavbar";
import { LuCarFront } from "react-icons/lu";
import { useEffect, useState, type FormEvent } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  searchProviders,
  type FreightModality,
  type ProviderSearchResult,
  type ProviderSearchFilters,
  type ProviderType,
} from "../features/provider-search/api";

import { getSession, readRecentSession, type Session } from "../features/auth/api";
import { DirectedRequestForm } from "../features/trip-planning/DirectedRequestForm";
import { readTripDraft } from "../features/trip-planning/draft";
import type { TripNavigationState } from "../features/trip-planning/criteria";

const dateTimeFormat = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });
const currencyFormat = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

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
  onSelect?: () => void;
  selected: boolean;
}

function ProviderCard({ provider, onAction, onSelect, selected }: ProviderCardProps) {
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
        <p className="font-semibold">{onSelect ? "Veículos compatíveis" : "Veículos disponíveis"}</p>
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
          aria-pressed={onSelect ? selected : undefined}
          onClick={onSelect ?? (() => onAction("O envio de solicitações ainda não está conectado."))}
          className="h-[42px] min-w-[194px] rounded-[8px] bg-[#17695b] px-[18px] text-[13px] font-semibold text-white cursor-pointer transition-colors hover:bg-[#12584d] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#17695b]"
        >
          {onSelect ? (selected ? "Prestador selecionado" : "Selecionar prestador") : "Solicitar a este prestador"}
        </button>
        {!onSelect && <button
          type="button"
          onClick={() =>
            onAction("Os detalhes estarão disponíveis quando houver dados do prestador.")
          }
          className="h-[42px] min-w-[102px] rounded-[8px] border border-[#dfe7e8] bg-white px-[18px] text-[13px] font-semibold text-[#17695b] cursor-pointer transition-colors hover:bg-[#f5f7f8] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#17695b]"
        >
          Ver dados
        </button>}
      </div>
    </article>
  );
}

export default function ClientePrestadores() {
  const { key, state } = useLocation();
  const [session, setSession] = useState<Session | null | undefined>(readRecentSession);
  const [sessionError, setSessionError] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    getSession(controller.signal).then(account => {
      if (!controller.signal.aborted) setSession(account);
    }).catch(() => {
      if (!controller.signal.aborted) setSessionError(true);
    });
    return () => controller.abort();
  }, []);

  const trip = session?.passageiro
    ? readTripDraft(session.id) ?? (state as TripNavigationState | null)?.trip
    : undefined;

  return (
    <div className="min-h-screen bg-[#f5f7f8] text-[#1d2d32]">
      <AccountNavbar fixed={false} currentSession={session ?? undefined} />
      {sessionError ? (
        <main className="mx-auto max-w-[1440px] p-6">
          <p role="alert">Não foi possível recuperar a sessão. Recarregue a página.</p>
        </main>
      ) : session === undefined ? (
        <main className="mx-auto max-w-[1440px] p-6" aria-busy="true">
          <div className="rounded-xl border border-slate-200 bg-white p-5">
            <p role="status">Carregando consulta…</p>
          </div>
        </main>
      ) : (
        <ProviderResults key={`${key}:${session?.id ?? "visitante"}`} state={trip ? { trip } : null} />
      )}
    </div>
  );
}

function ProviderResults({ state }: { state: TripNavigationState | null }) {
  const trip = state?.trip;
  const tripFilters: ProviderSearchFilters | undefined = trip
    ? { modalidade: "PESSOA", quantidadePassageiros: trip.passengers }
    : undefined;
  const [service, setService] = useState<FreightModality | "">(trip ? "PESSOA" : "");
  const [region, setRegion] = useState("");
  const [providerType, setProviderType] = useState<ProviderType | "">("");
  const [request, setRequest] = useState(tripFilters);
  const [result, setResult] = useState<{
    request: ProviderSearchFilters;
    providers: ProviderSearchResult[];
    error?: string;
  }>();
  const [sortByName, setSortByName] = useState(false);
  const [selectedVehicleId, setSelectedVehicleId] = useState<number>();
  const [feedback, setFeedback] = useState("");

  useEffect(() => {
    if (trip) window.scrollTo(0, 0);
  }, [trip]);

  useEffect(() => {
    if (!request) return;
    const controller = new AbortController();
    searchProviders(request, controller.signal).then(providers => {
      if (!controller.signal.aborted) setResult({ request, providers });
    }).catch((error: unknown) => {
      if (controller.signal.aborted) return;
      setResult({
        request,
        providers: [],
        error: error instanceof Error ? error.message : "Não foi possível consultar os prestadores.",
      });
    });
    return () => controller.abort();
  }, [request]);

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSelectedVehicleId(undefined);
    setRequest({
      ...parseRegion(region),
      ...(trip ? tripFilters : service && { modalidade: service }),
      ...(providerType && { tipo: providerType }),
    });
  }

  function clearFilters() {
    setRegion("");
    setProviderType("");
    setSortByName(false);
    setSelectedVehicleId(undefined);
    setRequest(tripFilters);
  }

  const loading = Boolean(request && result?.request !== request);
  const error = loading ? undefined : result?.error;
  const matches = loading ? [] : result?.providers ?? [];
  const providers = sortByName
    ? [...matches].sort((a, b) => a.nome.localeCompare(b.nome, "pt-BR"))
    : matches;
  const resultCount = providers.length;
  const vehicles = trip ? providers.flatMap(provider =>
    provider.veiculosDisponiveis.map(vehicle => ({ provider, vehicle })),
  ) : [];
  const selected = vehicles.find(({ vehicle }) => vehicle.id === selectedVehicleId);
  const FilterPanel = trip ? "details" : "section";

  return (
    <div className="[font-family:Inter,'Noto_Sans',Arial,sans-serif]">
      <main className={`mx-auto flex w-full max-w-[1440px] flex-col gap-6 px-12 pb-[79px] pt-9 max-lg:px-8 max-md:px-4 max-md:pb-12 max-md:pt-6 ${trip ? "lg:grid lg:grid-cols-[minmax(240px,1fr)_minmax(0,3fr)] lg:items-start" : ""}`}>
        {trip && (
          <section aria-labelledby="trip-summary" className="grid gap-3 rounded-[14px] border border-[#dfe7e8] bg-white p-6 wrap-anywhere lg:col-span-2">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 id="trip-summary" className="text-lg font-semibold">Seu roteiro</h2>
              <Link className="font-semibold text-[#17695b] underline" to="/cliente/planejar-viagem" state={state}>Editar itinerário</Link>
            </div>
            <p>{trip.returnAt ? "Ida e volta" : "Ida"} · {trip.passengers} {trip.passengers === 1 ? "passageiro" : "passageiros"}</p>
            <p>Partida em {dateTimeFormat.format(new Date(trip.departure))}</p>
            {trip.returnAt && <p>Retorno em {dateTimeFormat.format(new Date(trip.returnAt))}</p>}
            <ol className="list-decimal space-y-1 pl-5">
              <li>Embarque – {trip.origin}</li>
              {trip.stops.map((stop, index) => <li key={index}>Parada {index + 1} – {stop}</li>)}
              <li>Destino – {trip.destination}</li>
            </ol>
            {trip.offer && <div>
              <h3 className="font-semibold">Oferta original</h3>
              <p>{currencyFormat.format(Number(trip.offer.amount))}</p>
              <p className="whitespace-pre-wrap">{trip.offer.terms}</p>
            </div>}
          </section>
        )}
        <section
          aria-label="Consultar prestadores"
          className={`flex min-w-0 flex-col gap-6 ${trip ? "lg:row-span-3 lg:row-start-2 lg:col-start-1" : ""}`}
        >
          <div className="flex flex-col gap-6">
            <h1 className="text-[30px] font-bold leading-[1.45] text-[#1d2d32]">
              {trip ? "Veículos para sua viagem" : "Encontre o prestador certo"}
            </h1>
            <p className="text-[14px] font-normal leading-5 text-[#637479]">
              {trip
                ? "A disponibilidade para o período precisa ser confirmada pelo prestador."
                : "Pesquise por modalidade e região atendida antes de direcionar sua solicitação."}
            </p>
          </div>

          <FilterPanel open={trip ? true : undefined} className="w-full space-y-4 rounded-[14px] border border-[#dfe7e8] bg-white p-6">
            {trip
              ? <summary className="cursor-pointer text-[19px] font-semibold">Filtros</summary>
              : <h2 className="text-[19px] font-semibold leading-[1.45] text-[#1d2d32]">Como podemos ajudar?</h2>}
            <form
              onSubmit={handleSearch}
              className={`grid w-full items-end gap-5 ${trip ? "md:max-lg:grid-cols-[repeat(3,minmax(0,1fr))_99px]" : "md:grid-cols-[repeat(3,minmax(0,1fr))_99px]"}`}
            >
              <label className="flex min-w-0 flex-col gap-2 text-[12px] font-semibold leading-[17px] text-[#637479]" htmlFor="service">
                Modalidade
                <select
                  id="service"
                  disabled={Boolean(trip)}
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
                label={trip ? "Localização do prestador" : "Região de atendimento"}
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
                className="h-[46px] rounded-[8px] bg-[#17695b] px-[18px] text-[13px] font-semibold text-white cursor-pointer transition-colors hover:bg-[#12584d] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#17695b] max-md:w-full"
              >
                Pesquisar
              </button>
            </form>
            {trip && <div className="flex flex-wrap items-center justify-between gap-3">
              <button type="button" onClick={clearFilters} className="cursor-pointer text-sm font-semibold text-[#17695b] underline transition-colors hover:text-[#0e4940] hover:decoration-2 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#17695b]">Limpar filtros</button>
              <label className="flex flex-wrap items-center gap-2 text-sm">
                Ordenar por
                <select value={sortByName ? "nome" : ""} onChange={event => setSortByName(event.target.value === "nome")}
                  className="rounded-lg border border-[#dfe7e8] bg-white px-2 py-1">
                  <option value="">Ordem da consulta</option>
                  <option value="nome">Nome do prestador</option>
                </select>
              </label>
            </div>}
          </FilterPanel>
        </section>

        <h2 aria-live={trip ? "polite" : undefined} className={`text-[17px] font-semibold leading-[1.45] text-[#1d2d32] ${trip ? "lg:col-start-2" : ""}`}>
          {loading
            ? "Consultando prestadores..."
            : trip ? `${vehicles.length} ${vehicles.length === 1 ? "veículo compatível" : "veículos compatíveis"}`
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
        ) : trip && vehicles.length > 0 ? (
          <section aria-label="Veículos compatíveis" className="grid min-w-0 gap-3 lg:col-start-2">
            {vehicles.map(({ provider, vehicle }) => (
              <article key={vehicle.id} className="grid min-w-0 gap-4 rounded-xl border border-[#dfe7e8] bg-white p-5 sm:grid-cols-[auto_minmax(0,1fr)]">
                <span aria-hidden="true" className="grid size-12 place-items-center rounded-lg bg-[#e5f2ee] text-[#17695b]">
                  <LuCarFront size={24} />
                </span>
                <div className="min-w-0">
                  <h3 className="break-words text-lg font-semibold">{vehicle.marca} {vehicle.modelo}</h3>
                  <p className="mt-1 break-words text-sm text-slate-600">{provider.nome} · {provider.tipo === "EMPRESA" ? "Empresa" : "Autônomo"}</p>
                  <p className="mt-1 text-sm">Até {vehicle.capacidadePassageiros} {vehicle.capacidadePassageiros === 1 ? "passageiro" : "passageiros"}</p>
                  <details className="mt-3 text-sm">
                    <summary className="w-fit cursor-pointer font-semibold text-[#17695b]">Ver detalhes</summary>
                    <p className="mt-2">Ano do veículo: {vehicle.ano}</p>
                    {provider.regioesAtendidas.length > 0 && <p className="mt-1 break-words">
                      Regiões atendidas: {provider.regioesAtendidas.map(({ cidade, estado }) => `${cidade} - ${estado}`).join(", ")}
                    </p>}
                    <button type="button" onClick={() => {
                      setSelectedVehicleId(vehicle.id);
                      document.getElementById("request-review")?.scrollIntoView({ block: "nearest" });
                    }}
                      className="mt-3 inline-flex min-h-10 items-center rounded-lg bg-[#17695b] px-3 font-semibold text-white">
                      Revisar solicitação para {provider.nome}
                    </button>
                  </details>
                </div>
              </article>
            ))}
          </section>
        ) : !trip && resultCount > 0 ? (
          <section
            aria-label={trip ? "Prestadores compatíveis" : "Prestadores que atendem sua rota"}
            className="grid w-full grid-cols-2 items-start gap-5 max-md:grid-cols-1"
          >
            {providers.map((provider) => (
              <ProviderCard
                key={provider.id}
                provider={provider}
                onAction={setFeedback}
                selected={false}
              />
            ))}
          </section>
        ) : (
          <section
            aria-label="Prestadores disponíveis"
            className="rounded-[14px] border border-[#dfe7e8] bg-white p-6"
          >
            <p className="text-[14px] leading-[1.45] text-[#637479]">
              {request
                ? trip ? "Nenhum veículo compatível para os filtros informados." : "Nenhum prestador encontrado para os filtros informados."
                : "Informe os filtros desejados e pesquise para consultar os prestadores."}
            </p>
          </section>
        )}

        {trip && <section aria-labelledby="request-review" className="scroll-mt-6 lg:col-start-2 rounded-[14px] border border-[#dfe7e8] bg-white p-6">
          <h2 id="request-review" className="text-lg font-semibold">Revisar solicitação</h2>
          <p role="status" className="mt-2">{selected ? `Destinatário: ${selected.provider.nome}` : "Selecione um veículo para revisar o destinatário."}</p>
          {selected && <DirectedRequestForm key={selected.vehicle.id} trip={trip}
            providerId={selected.provider.id} vehicleId={selected.vehicle.id} />}
        </section>}
        {!trip && <section className="flex min-h-[172px] w-full flex-col items-start gap-4 rounded-[14px] border border-[#dfe7e8] bg-white p-6">
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
            className="h-[42px] rounded-[8px] border border-[#dfe7e8] bg-white px-[18px] text-[13px] font-semibold text-[#17695b] cursor-pointer transition-colors hover:bg-[#f5f7f8] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#17695b]"
          >
            Criar solicitação aberta
          </button>
        </section>}
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
