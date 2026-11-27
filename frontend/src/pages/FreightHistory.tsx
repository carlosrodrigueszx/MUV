import { ProviderLayout } from "../components/layout/ProviderLayout";
import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import {
  AccountNavbar,
  FreightHistoryApiError,
  HistoryDetails,
  HistoryFilters,
  HistoryList,
  createFreightHistoryService,
  type AccountProfile,
  type FreightHistoryFilters,
  type FreightHistoryRecord,
} from "../features/freight-history";

function initialFilters(profile: FreightHistoryFilters["profile"], month = "2026-11"): FreightHistoryFilters {
  return {
    profile,
    modality: "todas",
    status: "todas",
    month,
  };
}

export default function FreightHistory({ fretesEmpresa = false }: { fretesEmpresa?: boolean }) {
  const location = useLocation();
  const routeProfile: FreightHistoryFilters["profile"] = location.pathname.startsWith("/prestador/")
    ? "prestador"
    : "cliente";

  const query = new URLSearchParams(location.search);
  const freight = query.get("frete");
  const request = query.get("solicitacao");
  const period = freight || request ? query.get("periodo") : null;
  return <FreightHistoryContent
    key={location.pathname + location.search}
    fretesEmpresa={fretesEmpresa}
    routeProfile={routeProfile}
    preferredId={freight ? `FR-${freight}` : request ? `SL-${request}` : null}
    period={period && /^\d{4}-(0[1-9]|1[0-2])$/.test(period) ? period : undefined}
  />;
}

function FreightHistoryContent({
  fretesEmpresa,
  routeProfile,
  preferredId,
  period,
}: {
  fretesEmpresa: boolean;
  routeProfile: FreightHistoryFilters["profile"];
  preferredId: string | null;
  period?: string;
}) {
  const navigate = useNavigate();
  const historyService = useMemo(
    () =>
      createFreightHistoryService({
        baseUrl: import.meta.env.VITE_API_URL ?? "",
      }),
    [],
  );
  const [draftFilters, setDraftFilters] = useState<FreightHistoryFilters>(() => fretesEmpresa
    ? { ...initialFilters(routeProfile, ""), status: "Contratado" }
    : initialFilters(routeProfile, period));
  const [appliedFilters, setAppliedFilters] = useState(draftFilters);
  const [records, setRecords] = useState<FreightHistoryRecord[]>([]);
  const [allowedProfiles, setAllowedProfiles] = useState<AccountProfile[] | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(preferredId);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    void historyService
      .profiles(controller.signal)
      .then((profiles) => {
        if (profiles.length === 0) {
          throw new Error("A conta autenticada não possui um perfil disponível para consulta.");
        }
        setAllowedProfiles(profiles);
        if (!profiles.includes(routeProfile)) setLoading(false);
      })
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        if (requestError instanceof FreightHistoryApiError && requestError.status === 401) {
          navigate("/entrar", {
            replace: true,
            state: { notice: "Entre na sua conta para consultar o histórico de fretes." },
          });
          return;
        }
        setLoading(false);
        setError(
          requestError instanceof Error
            ? requestError.message
            : "Não foi possível verificar os perfis disponíveis.",
        );
      });

    return () => controller.abort();
  }, [historyService, navigate, routeProfile]);

  useEffect(() => {
    if (!allowedProfiles || !allowedProfiles.includes(appliedFilters.profile)) return;

    const controller = new AbortController();

    void historyService
      .list(appliedFilters, controller.signal)
      .then((result) => {
        const visible = fretesEmpresa ? result.filter((record) => record.id.startsWith("FR-")) : result;
        setRecords(visible);
        setSelectedId((current) =>
          visible.some((record) => record.id === current) ? current : (visible[0]?.id ?? null),
        );
      })
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
        if (requestError instanceof FreightHistoryApiError && requestError.status === 401) {
          navigate("/entrar", {
            replace: true,
            state: { notice: "Entre na sua conta para consultar o histórico de fretes." },
          });
          return;
        }
        setRecords([]);
        setSelectedId(null);
        setError(
          requestError instanceof Error
            ? requestError.message
            : "Não foi possível consultar o histórico de fretes.",
        );
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [allowedProfiles, appliedFilters, fretesEmpresa, historyService, navigate, requestVersion]);

  const alternateProfile = allowedProfiles && !allowedProfiles.includes(routeProfile)
    ? allowedProfiles[0] : undefined;
  const selectedRecord = records.find((record) => record.id === selectedId) ?? null;
  const { hash } = useLocation();
  useEffect(() => {
    if (!loading && selectedRecord?.id === preferredId && hash === "#freight-details") {
      document.getElementById("freight-details")?.scrollIntoView();
    }
  }, [hash, loading, preferredId, selectedRecord]);

  const content = <>
    {!fretesEmpresa && <p className="text-[11px] font-bold tracking-wide text-[#176A5B]">RF014 / MUV</p>}
    <h1 className="mt-7 text-3xl font-extrabold tracking-tight text-[#203139] sm:text-[34px]">
      {fretesEmpresa ? "Fretes da empresa" : "Seu histórico de fretes"}
    </h1>
    {!fretesEmpresa && <p className="mt-5 text-sm text-slate-500">
      Consulte solicitações, propostas e serviços da sua conta — como cliente ou prestador.
    </p>}

    {alternateProfile ? (
      <section className="mt-7 rounded-xl border border-slate-200 bg-white p-5 text-sm">
        <p role="status">
          {routeProfile === "cliente"
            ? "Esta conta não tem perfil de passageiro para consultar este histórico."
            : "Esta conta não tem perfil de prestador para consultar este histórico."}
        </p>
        <Link to={`/${alternateProfile}/historico`} className="mt-3 inline-block font-semibold text-[#176A5B] underline">
          {alternateProfile === "prestador" ? "Abrir histórico do prestador" : "Abrir histórico do passageiro"}
        </Link>
      </section>
    ) : <div className="mt-7 space-y-6">
      <HistoryFilters
        filters={draftFilters}
        fretesEmpresa={fretesEmpresa}
        allowedProfiles={allowedProfiles ?? [routeProfile]}
        onChange={setDraftFilters}
        onSubmit={() => {
          if (draftFilters.profile !== routeProfile) {
            navigate(`/${draftFilters.profile}/historico`);
            return;
          }
          setLoading(true);
          setError(null);
          setAppliedFilters(draftFilters);
          setRequestVersion((version) => version + 1);
        }}
      />
      {error && (
        <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-5 py-4 text-sm text-red-700">
          {error}
        </div>
      )}
      {loading ? (
        <div className="rounded-2xl border border-slate-200 bg-white px-6 py-12 text-center text-sm text-slate-500">
          Consultando o histórico de fretes...
        </div>
      ) : (
        <HistoryList records={records} selectedId={selectedId} onSelect={(record) => setSelectedId(record.id)} />
      )}
      <HistoryDetails record={selectedRecord} cliente={routeProfile === "cliente"} />
    </div>}
  </>;

  if (routeProfile === "prestador") {
    return <ProviderLayout pageTitle={fretesEmpresa ? "Fretes" : "Histórico"} activeNavKey={fretesEmpresa ? "freights" : "history"}
      profileLabel="Prestador" isOnline={null} passengerPortalHref="/cliente/planejar-viagem">
      {content}
    </ProviderLayout>;
  }

  return (
    <div className="min-h-screen bg-[#F4F7F8] text-slate-900 antialiased">
      <AccountNavbar />
      <main className="mx-auto max-w-[1440px] px-5 pb-10 pt-28 sm:px-8 lg:px-12">
        {content}
      </main>
    </div>
  );
}
