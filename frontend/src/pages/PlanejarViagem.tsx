import { AccountNavbar } from "../features/freight-history/components/AccountNavbar";
import { useEffect, useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { LuArrowRight, LuPlus, LuSearch, LuX } from "react-icons/lu";
import litoral from "../assets/viagens/litoral.webp";
import saoPaulo from "../assets/viagens/sao-paulo.webp";
import santos from "../assets/viagens/santos.webp";
import serra from "../assets/viagens/serra.webp";
import { getSession, readRecentSession, type Session } from "../features/auth/api";

import { validateTrip, type TripCriteria, type TripNavigationState } from "../features/trip-planning/criteria";

import { saveTripDraft } from "../features/trip-planning/draft";

const field = "w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-sm";
const primary = "inline-flex items-center justify-center gap-2 rounded-lg bg-[#176457] px-4 py-2.5 text-sm font-semibold text-white cursor-pointer transition-colors hover:bg-[#0e4940] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#176457]";
const label = "grid gap-1 text-sm font-medium";
const helper = "text-xs leading-relaxed text-slate-500";
const offerAmountFormat = new Intl.NumberFormat("pt-BR", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});
const routes = [
  { name: "São Paulo em boa companhia", detail: "City tour · São Paulo", price: "R$ 540 / grupo", image: saoPaulo },
  { name: "Um dia de mar em Santos", detail: "Litoral · Santos", price: "R$ 780 / grupo", image: santos },
  { name: "Pela serra, sem pressa", detail: "Rota panorâmica", price: "R$ 690 / grupo", image: serra },
];

export default function PlanejarViagem() {
  const navigate = useNavigate();
  const { hash, key, state } = useLocation();
  const [session, setSession] = useState<Session | undefined>(readRecentSession);
  const [sessionError, setSessionError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    getSession(controller.signal).then(account => {
      if (controller.signal.aborted) return;
      if (account) setSession(account);
      else navigate("/entrar", { replace: true });
    }).catch(() => {
      if (!controller.signal.aborted) setSessionError("Não foi possível verificar sua sessão. Recarregue a página.");
    });
    return () => controller.abort();
  }, [navigate]);

  useEffect(() => {
    if (session && hash === "#roteiros") {
      document.getElementById("roteiros")?.scrollIntoView();
    }
  }, [hash, session]);

  if (!session) {
    return <main className="p-6 text-slate-700">
      {sessionError ? <p role="alert">{sessionError}</p> : <p role="status">Verificando sessão…</p>}
    </main>;
  }

  if (!session.passageiro) {
    const destination = session.prestador ? "/prestador"
      : session.motorista ? "/motorista" : "/conta";
    return <Navigate to={destination} replace />;
  }

  const { trip, prestadorId } = (state as TripNavigationState | null) ?? {};
  return <TripPlanner key={`${session.id}:${key}`} session={session} sessionError={sessionError}
    trip={trip} prestadorId={prestadorId} />;
}

function TripPlanner({ session, sessionError, trip, prestadorId }: {
  session: Session;
  sessionError: string;
  trip?: TripCriteria;
  prestadorId?: number;
}) {
  const navigate = useNavigate();
  const [formError, setFormError] = useState("");
  const [offerAmount, setOfferAmount] = useState(() =>
    trip?.offer ? offerAmountFormat.format(Number(trip.offer.amount)) : "",
  );
  const [roundTrip, setRoundTrip] = useState(Boolean(trip?.returnAt));
  const [stops, setStops] = useState(trip?.stops ?? []);
  const [departure, setDeparture] = useState(trip?.departure.split("T")[0] ?? "");
  const [returnDate, setReturnDate] = useState(trip?.returnAt?.split("T")[0] ?? "");
  const [returnTime, setReturnTime] = useState(trip?.returnAt?.split("T")[1] ?? "");

  return <main className="min-h-screen bg-[#f4f6f7] text-slate-900">
    <AccountNavbar currentSession={session} fixed={false} />
    {sessionError && <p role="alert" className="px-4 py-2 text-sm text-red-800">{sessionError}</p>}

    <div className="mx-auto grid max-w-[1440px] gap-5 px-4 py-6 sm:px-8 lg:px-12 lg:pt-7">
      <section className="grid overflow-hidden rounded-3xl bg-[#e4f0eb] shadow-[0_4px_20px_#142d251a] md:min-h-[267px] md:grid-cols-[47.6%_52.4%]">
        <div className="px-6 py-7 sm:px-9 sm:py-8">
          <p className="text-[11px] font-bold text-[#00695b]">SEU GRUPO. SEU ROTEIRO. SEU TEMPO.</p>
          <h1 className="mt-3.5 max-w-[440px] text-3xl leading-[1.12] font-bold tracking-[-0.035em] text-[#005447] lg:text-[42px]">Defina seu roteiro e escolha o motorista.</h1>
          <p className="mt-4 text-sm leading-5 text-[#5c717a]">Você define o trajeto, as paradas e o horário. Depois, escolhe o motorista para sua viagem.</p>
        </div>
        <div className="relative h-44 md:h-auto">
          <img src={litoral} alt="" className="absolute inset-0 h-full w-full object-cover" />
        </div>
      </section>

      <form className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6"
        onInput={() => setFormError("")}
        onSubmit={event => {
          event.preventDefault();
          const form = event.currentTarget;
          const data = new FormData(form);
          data.set("valor", offerAmount.replaceAll(".", "").replace(",", "."));
          const result = validateTrip(data, roundTrip);
          if (result.error) {
            setFormError(result.error);
            const fields = form.querySelectorAll<HTMLInputElement | HTMLTextAreaElement>(`[name="${result.field}"]`);
            const invalidField = result.field === "parada"
              ? Array.from(fields).find(input => !input.value.trim())
              : fields[0];
            invalidField?.focus();
            return;
          }
          // Keep the submitted criteria on both entries so browser Back also restores them.
          saveTripDraft(session.id, result.trip);
          const state = { trip: result.trip, prestadorId };
          navigate("/cliente/planejar-viagem", { replace: true, state });
          navigate("/cliente/prestadores", { state });
        }}>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-xl font-bold">Defina seu roteiro</h2>
          <fieldset className="flex gap-1 rounded-lg bg-slate-100 p-1 text-sm">
            <legend className="sr-only">Tipo de viagem</legend>
            <label className="cursor-pointer rounded-md px-3 py-1.5 has-checked:bg-white has-checked:font-semibold has-checked:text-[#176457] has-focus-visible:outline">
              <input className="sr-only" type="radio" name="trajeto" checked={!roundTrip} onChange={() => setRoundTrip(false)} /> Ida
            </label>
            <label className="cursor-pointer rounded-md px-3 py-1.5 has-checked:bg-white has-checked:font-semibold has-checked:text-[#176457] has-focus-visible:outline">
              <input className="sr-only" type="radio" name="trajeto" checked={roundTrip} onChange={() => setRoundTrip(true)} /> Ida e volta
            </label>
          </fieldset>
        </div>
        <label className={label}>Embarque<input className={field} name="embarque" defaultValue={trip?.origin} required placeholder="Informe o ponto de embarque" /></label>
        <p className="text-sm font-medium">Paradas</p>
        {stops.map((stop, index) => <div key={index} className="flex items-center gap-3 rounded-lg border border-slate-200 p-2">
          <span aria-hidden="true" className="grid size-7 shrink-0 place-items-center rounded-full bg-[#e5f1eb] text-sm font-semibold text-[#176457]">{index + 1}</span>
          <label className={`${label} flex-1`}>Parada {index + 1}
            <input className={field} name="parada" required value={stop} onChange={event => {
              const value = event.target.value;
              setStops(current => current.map((item, position) => position === index ? value : item));
            }} />
          </label>
          <button type="button" aria-label={`Remover parada ${index + 1}`} className="cursor-pointer rounded-lg bg-slate-100 p-3 transition-colors hover:bg-red-50 hover:text-red-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#176457]"
            onClick={() => setStops(current => current.filter((_, position) => position !== index))}>
            <LuX aria-hidden="true" />
          </button>
        </div>)}
        <button type="button" className="flex items-center gap-2 cursor-pointer rounded-lg bg-slate-100 px-3 py-2 text-left text-sm text-[#176457] transition-colors hover:bg-[#e2f1ed] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#176457]"
          onClick={() => setStops(current => [...current, ""])}><LuPlus aria-hidden="true" /> Adicionar outra parada</button>
        <label className={label}>Destino<input className={field} name="destino" defaultValue={trip?.destination} required placeholder="Informe o destino" /></label>
        <div className={`grid grid-cols-1 gap-3 sm:grid-cols-2 ${roundTrip ? "lg:grid-cols-5" : "lg:grid-cols-3"}`}>
          <label className={label}>Data de ida<input className={field} type="date" name="dataIda" required value={departure} onChange={event => setDeparture(event.target.value)} /></label>
          <label className={label}>Horário de ida<input className={field} type="time" name="horaIda" defaultValue={trip?.departure.split("T")[1]} required /></label>
          {roundTrip && <>
            <label className={label}>Data de retorno<input className={field} type="date" name="dataRetorno" value={returnDate} onChange={event => setReturnDate(event.target.value)} required min={departure || undefined} /></label>
            <label className={label}>Horário de retorno<input className={field} type="time" name="horaRetorno" value={returnTime} onChange={event => setReturnTime(event.target.value)} required /></label>
          </>}
          <label className={label}>Passageiros<input className={field} type="number" name="passageiros" min="1" max="2147483647" defaultValue={trip?.passengers ?? 1} required /></label>
        </div>
        <fieldset className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]">
          <legend className="mb-2 font-semibold">Oferta original (opcional)</legend>
          <div>
            <label htmlFor="valor-oferta" className={label}>Valor em reais</label>
            <input
              id="valor-oferta"
              className={field + " mt-1"}
              name="valor"
              type="text"
              inputMode="numeric"
              placeholder="R$ 0,00"
              aria-describedby="ajuda-valor-oferta"
              value={offerAmount}
              onChange={event => {
                const entered = event.target.value;
                const digits = entered.replace(/\D/g, "").replace(/^0+(?=\d)/, "");
                if (digits.length > 13) return;
                const clearedZero = offerAmount === "0,00" && entered.length < offerAmount.length && digits === "0";
                setOfferAmount(digits && !clearedZero ? offerAmountFormat.format(Number(digits) / 100) : "");
              }}
            />
            <p id="ajuda-valor-oferta" className={helper + " mt-1"}>
              Informe o valor total, considerando o roteiro e todos os passageiros. Use-o como referência para comparar propostas. Nenhum pagamento é feito nesta etapa.
            </p>
          </div>
          <div>
            <label htmlFor="condicoes-oferta" className={label}>Condições e detalhes da oferta</label>
            <textarea
              id="condicoes-oferta"
              className={field + " mt-1"}
              name="condicoes"
              placeholder="Ex.: inclui bagagem extra, tempo de espera de 1h, pagamento no PIX ou reembolso até 24h antes"
              aria-describedby="ajuda-condicoes-oferta"
              defaultValue={trip?.offer?.terms}
            />
            <p id="ajuda-condicoes-oferta" className={helper + " mt-1"}>
              Informe o que está incluso ou regras importantes da proposta para orientar a cotação.
            </p>
          </div>
        </fieldset>
        {formError && <p role="alert" className="text-sm text-red-700">{formError}</p>}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <button className={primary}><LuSearch aria-hidden="true" /> Consultar prestadores</button>
        </div>
      </form>

      <section className="rounded-2xl bg-[#006e5e] p-5 text-white sm:p-7" aria-labelledby="roteiros">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h2 id="roteiros" className="text-xl font-bold">Roteiros prontos para aproveitar</h2>
            <p className="mt-1 text-xs text-white/90">Inspire-se nesses roteiros e personalize o trajeto com suas próprias paradas.</p>
          </div>
          <a href="#pacotes" className="inline-flex items-center gap-2 rounded-lg bg-white px-4 py-3 text-xs font-semibold text-[#006e5e] transition-colors hover:bg-[#e2f1eb] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white">
            <LuArrowRight size={16} aria-hidden="true" /> Explorar pacotes
          </a>
        </div>
        <ul id="pacotes" tabIndex={-1} className="mt-7 grid scroll-mt-6 gap-5 md:grid-cols-3">
          {routes.map(route => <li key={route.name} className="flex items-center gap-3.5 rounded-xl border border-[#dfe6e7] bg-white p-3 text-[#1d2d32]">
            <img src={route.image} alt="" loading="lazy" className="h-24 w-24 shrink-0 rounded-lg object-cover md:h-20 md:w-20 lg:h-[108px] lg:w-[116px]" />
            <div className="min-w-0">
              <h3 className="text-sm font-semibold">{route.name}</h3>
              <p className="mt-1.5 text-xs text-[#5c717a]">{route.detail}</p>
              <p className="mt-2 text-base font-bold text-[#006e5e]">{route.price}</p>
            </div>
          </li>)}
        </ul>
      </section>
    </div>
  </main>;
}
