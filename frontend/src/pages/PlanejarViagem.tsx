import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { LuBus, LuPlus, LuSearch, LuX } from "react-icons/lu";
import hero from "../assets/hero.png";
import { getSession, type Session } from "../features/auth/api";

const field = "w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-sm";
const primary = "inline-flex items-center justify-center gap-2 rounded-lg bg-[#176457] px-4 py-2.5 text-sm font-semibold text-white hover:bg-[#134f45]";
const label = "grid gap-1 text-sm font-medium";

export default function PlanejarViagem() {
  const navigate = useNavigate();
  const [session, setSession] = useState<Session>();
  const [sessionError, setSessionError] = useState("");
  const [roundTrip, setRoundTrip] = useState(false);
  const [stops, setStops] = useState<string[]>([]);
  const [departure, setDeparture] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    getSession(controller.signal).then(account => {
      if (account) setSession(account);
      else navigate("/entrar", { replace: true });
    }).catch(() => {
      if (!controller.signal.aborted) setSessionError("Não foi possível verificar sua sessão. Recarregue a página.");
    });
    return () => controller.abort();
  }, [navigate]);

  if (!session) {
    return <main className="p-6 text-slate-700">
      {sessionError ? <p role="alert">{sessionError}</p> : <p role="status">Verificando sessão…</p>}
    </main>;
  }

  return <main className="min-h-screen bg-[#f4f6f7] text-slate-900">
    <header className="border-b border-slate-200 bg-white">
      <nav aria-label="Menu principal" className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-4 py-3 sm:px-6">
        <Link to="/" className="flex items-center gap-2 font-bold text-[#164f44]">
          <LuBus aria-hidden="true" /> MUV
        </Link>
        <div className="flex flex-wrap items-center gap-4 text-sm">
          <Link to="/cliente/planejar-viagem" aria-current="page" className="font-semibold text-[#176457]">Planejar viagem</Link>
          <Link to="/cliente/historico">Minhas viagens</Link>
        </div>
        <Link to="/conta" title={session.nome} className="rounded-full bg-[#e1f1eb] px-3 py-2 text-sm font-semibold text-[#164f44]">Minha conta</Link>
      </nav>
    </header>

    <div className="mx-auto grid max-w-6xl gap-5 px-4 py-6 sm:px-6">
      <section className="grid overflow-hidden rounded-2xl bg-[#e5f1eb] shadow-sm md:grid-cols-2">
        <div className="self-center p-6 sm:p-8">
          <h1 className="text-3xl font-bold tracking-tight text-[#164f44] sm:text-4xl">Defina seu roteiro e escolha o motorista.</h1>
          <p className="mt-3 max-w-md text-sm text-slate-700">Planeje o trajeto, as paradas e os horários. Depois, consulte os prestadores na sua região.</p>
        </div>
        <img src={hero} alt="" className="h-44 w-full object-cover md:h-full md:max-h-64" />
      </section>

      <form className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6"
        onSubmit={event => {
          event.preventDefault();
          navigate("/cliente/prestadores");
        }}>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-xl font-bold">Defina seu roteiro</h2>
          <fieldset className="flex gap-3 rounded-lg bg-slate-100 px-3 py-2 text-sm">
            <legend className="sr-only">Tipo de viagem</legend>
            <label><input type="radio" name="trajeto" checked={!roundTrip} onChange={() => setRoundTrip(false)} /> Ida</label>
            <label><input type="radio" name="trajeto" checked={roundTrip} onChange={() => setRoundTrip(true)} /> Ida e volta</label>
          </fieldset>
        </div>
        <label className={label}>Embarque<input className={field} name="embarque" required placeholder="Informe o ponto de embarque" /></label>
        {stops.map((stop, index) => <div key={index} className="flex items-end gap-2">
          <label className={`${label} flex-1`}>Parada {index + 1}
            <input className={field} required value={stop} onChange={event => {
              const value = event.target.value;
              setStops(current => current.map((item, position) => position === index ? value : item));
            }} />
          </label>
          <button type="button" aria-label={`Remover parada ${index + 1}`} className="rounded-lg bg-slate-100 p-3"
            onClick={() => setStops(current => current.filter((_, position) => position !== index))}>
            <LuX aria-hidden="true" />
          </button>
        </div>)}
        <button type="button" className="flex items-center gap-2 rounded-lg bg-slate-100 px-3 py-2 text-left text-sm text-[#176457]"
          onClick={() => setStops(current => [...current, ""])}><LuPlus aria-hidden="true" /> Adicionar parada</button>
        <label className={label}>Destino<input className={field} name="destino" required placeholder="Informe o destino" /></label>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <label className={label}>Data de ida<input className={field} type="date" name="dataIda" required value={departure} onChange={event => setDeparture(event.target.value)} /></label>
          <label className={label}>Horário de ida<input className={field} type="time" name="horaIda" required /></label>
          {roundTrip && <>
            <label className={label}>Data de retorno<input className={field} type="date" name="dataRetorno" required min={departure || undefined} /></label>
            <label className={label}>Horário de retorno<input className={field} type="time" name="horaRetorno" required /></label>
          </>}
          <label className={label}>Passageiros<input className={field} type="number" name="passageiros" min="1" defaultValue="1" required /></label>
        </div>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm text-slate-600">O roteiro fica nesta tela. A próxima página permite pesquisar prestadores por modalidade e região.</p>
          <button className={primary}><LuSearch aria-hidden="true" /> Consultar prestadores</button>
        </div>
      </form>
    </div>
  </main>;
}
