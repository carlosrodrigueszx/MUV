import { useEffect, useMemo, useState } from "react";
import {
  AccountNavbar,
  HistoryDetails,
  HistoryFilters,
  HistoryList,
  createFreightHistoryService,
  type FreightHistoryFilters,
  type FreightHistoryRecord,
} from "../features/freight-history";

const initialFilters: FreightHistoryFilters = {
  profile: "cliente",
  modality: "todas",
  status: "todas",
  month: "2026-11",
};

export default function FreightHistory() {
  const historyService = useMemo(
    () =>
      createFreightHistoryService({
        baseUrl: import.meta.env.VITE_API_URL ?? "",
        // Adaptador temporário e isolado: o RF001 poderá fornecer estes valores
        // sem alterar o contrato do serviço nem os componentes desta tela.
        getUserId: () =>
          sessionStorage.getItem("muv_user_id") ?? import.meta.env.VITE_USER_ID ?? null,
        getAuthToken: () => sessionStorage.getItem("muv_access_token"),
      }),
    [],
  );
  const [draftFilters, setDraftFilters] = useState(initialFilters);
  const [appliedFilters, setAppliedFilters] = useState(initialFilters);
  const [records, setRecords] = useState<FreightHistoryRecord[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [requestVersion, setRequestVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    void historyService
      .list(appliedFilters, controller.signal)
      .then((result) => {
        setRecords(result);
        setSelectedId((current) =>
          result.some((record) => record.id === current) ? current : (result[0]?.id ?? null),
        );
      })
      .catch((requestError: unknown) => {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return;
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
  }, [appliedFilters, historyService, requestVersion]);

  const selectedRecord = records.find((record) => record.id === selectedId) ?? null;

  return (
    <div className="min-h-screen bg-[#F4F7F8] text-slate-900 antialiased">
      <AccountNavbar />
      <main className="mx-auto max-w-[1440px] px-5 pb-10 pt-28 sm:px-8 lg:px-12">
        <p className="text-[11px] font-bold tracking-wide text-[#176A5B]">RF014 / MUV</p>
        <h1 className="mt-7 text-3xl font-extrabold tracking-tight text-[#203139] sm:text-[34px]">
          Seu histórico de fretes
        </h1>
        <p className="mt-5 text-sm text-slate-500">
          Consulte solicitações, propostas e serviços da sua conta — como cliente ou prestador.
        </p>

        <div className="mt-7 space-y-6">
          <HistoryFilters
            filters={draftFilters}
            onChange={setDraftFilters}
            onSubmit={() => {
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
          <HistoryDetails record={selectedRecord} />
        </div>

        
      </main>
    </div>
  );
}
