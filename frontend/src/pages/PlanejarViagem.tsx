import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { LuBus, LuPlus, LuSearch, LuX } from "react-icons/lu";
import litoral from "../assets/viagens/litoral.webp";
import saoPaulo from "../assets/viagens/sao-paulo.webp";
import santos from "../assets/viagens/santos.webp";
import serra from "../assets/viagens/serra.webp";
import { getSession, type Session } from "../features/auth/api";

const field = "w-full rounded-lg border border-slate-200 bg-white px-3 py-2.5 text-sm";
const primary = "inline-flex items-center justify-center gap-2 rounded-lg bg-[#176457] px-4 py-2.5 text-sm font-semibold text-white hover:bg-[#134f45]";
const label = "grid gap-1 text-sm font-medium";
const routes = [
  { name: "São Paulo em boa companhia", detail: "City tour · São Paulo", image: saoPaulo },
  { name: "Um dia de mar em Santos", detail: "Litoral · Santos", image: santos },
  { name: "Pela serra, sem pressa", detail: "Roteiro panorâmico", image: serra },
];

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
          <Link to="/cliente/planejar-viagem" aria-current="page" className="border-b-2 border-[#176457] py-2 font-semibold text-[#176457]">Planejar viagem</Link>
          <Link to="/cliente/historico">Minhas viagens</Link>
        </div>
        <Link to="/conta" title={session.nome} className="rounded-full bg-[#e1f1eb] px-3 py-2 text-sm font-semibold text-[#164f44]">Minha conta</Link>
      </nav>
    </header>

    <div className="mx-auto grid max-w-6xl gap-5 px-4 py-6 sm:px-6">
      <section className="grid overflow-hidden rounded-2xl bg-[#e5f1eb] shadow-sm md:grid-cols-[48%_52%]">
        <div className="self-center p-5 sm:p-7">
          <h1 className="text-3xl font-bold tracking-tight text-[#164f44] sm:text-4xl">Defina seu roteiro e escolha o motorista.</h1>
          <p className="mt-3 max-w-md text-sm text-slate-700">Planeje o trajeto, as paradas e os horários. Depois, consulte os prestadores na sua região.</p>
        </div>
        <img src={litoral} alt="" className="h-44 w-full object-cover md:h-full md:min-h-48 md:max-h-56" />
      </section>

      <form className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6"
        onSubmit={event => {
          event.preventDefault();
          navigate("/cliente/prestadores");
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
        <label className={label}>Embarque<input className={field} name="embarque" required placeholder="Informe o ponto de embarque" /></label>
        <p className="text-sm font-medium">Paradas</p>
        {stops.map((stop, index) => <div key={index} className="flex items-center gap-3 rounded-lg border border-slate-200 p-2">
          <span aria-hidden="true" className="grid size-7 shrink-0 place-items-center rounded-full bg-[#e5f1eb] text-sm font-semibold text-[#176457]">{index + 1}</span>
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
          onClick={() => setStops(current => [...current, ""])}><LuPlus aria-hidden="true" /> Adicionar outra parada</button>
        <label className={label}>Destino<input className={field} name="destino" required placeholder="Informe o destino" /></label>
        <div className={`grid grid-cols-1 gap-3 sm:grid-cols-2 ${roundTrip ? "lg:grid-cols-5" : "lg:grid-cols-3"}`}>
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

      <section className="rounded-xl bg-[#176457] p-5 text-white" aria-labelledby="roteiros">
        <h2 id="roteiros" className="text-lg font-bold">Roteiros prontos para aproveitar</h2>
        <p className="mt-1 text-sm text-white/85">Inspire-se nestes exemplos e personalize o seu trajeto.</p>
        <ul className="mt-4 grid gap-3 md:grid-cols-3">
          {routes.map(route => <li key={route.name} className="flex items-center gap-3 rounded-lg bg-white p-2 text-slate-900">
            <img src={route.image} alt="" className="h-14 w-16 rounded-md object-cover" />
            <div>
              <h3 className="text-sm font-semibold">{route.name}</h3>
              <p className="text-xs text-slate-600">{route.detail}</p>
            </div>
          </li>)}
        </ul>
      </section>
    </div>
  </main>;
}
