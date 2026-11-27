import { useCallback, useEffect, useMemo, useState, type ChangeEvent, type FormEvent, type ReactNode } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { LuArchive, LuCheck, LuLayoutDashboard, LuPlus, LuSearch } from "react-icons/lu";
import { ProviderLayout } from "../../components/layout/ProviderLayout";
import type { ProviderNavItem } from "../../components/layout/providerNav";
import { confirmarDevolucao, consultarOpcoesAchado, listarAchados, registrarAchado, type Achado, type OpcoesAchado } from "./api";

const painel = "rounded-xl border border-[#dfe6e7] bg-white p-5 sm:p-6";
const campo = "mt-2 h-11 w-full rounded-lg border border-[#d8e2e5] bg-[#f8fafb] px-3 text-sm outline-none focus:border-[#17695b] focus:ring-2 focus:ring-[#17695b]/15";
const botao = "inline-flex min-h-11 items-center justify-center gap-2 rounded-lg border px-4 text-sm font-semibold focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#17695b] disabled:cursor-not-allowed disabled:opacity-55";
const primario = `${botao} border-[#17695b] bg-[#17695b] text-white hover:bg-[#104e43]`;
const secundario = `${botao} border-[#d8e2e5] bg-white text-[#17695b] hover:border-[#17695b]`;
const dataFormatada = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

function usePortal() {
  const isDriver = useLocation().pathname.startsWith("/motorista/");
  const base = isDriver ? "/motorista/achados-perdidos" : "/prestador/achados-perdidos";
  const navItems: ProviderNavItem[] = useMemo(() => [
    { key: "overview", label: "Visão geral", to: isDriver ? "/motorista" : "/prestador", icon: LuLayoutDashboard }, {
    key: "lost-found", label: "Achados e perdidos", to: base, icon: LuArchive,
  }], [base, isDriver]);
  return { isDriver, base, navItems };
}

function Layout({ title, children }: { title: string; children: ReactNode }) {
  const { isDriver, navItems } = usePortal();
  const area = isDriver ? "Área do motorista" : "Área do prestador";
  return <ProviderLayout pageTitle={title} activeNavKey="lost-found" navItems={navItems}
    portalLabel={isDriver ? "PORTAL DO MOTORISTA" : "PORTAL DO PRESTADOR"}
    areaLabel={area} organizationName="" profileLabel={isDriver ? "Motorista" : "Prestador"} isOnline={null}>
    {children}
  </ProviderLayout>;
}

function mensagem(error: unknown) {
  return error instanceof Error ? error.message : "Não foi possível concluir a operação.";
}

function Estado({ texto, erro, tentar }: { texto: string; erro?: boolean; tentar?: () => void }) {
  return <div className={`${painel} grid min-h-40 place-items-center gap-3 text-center`}>
    <p role={erro ? "alert" : "status"} className="text-sm text-[#637479]">{texto}</p>
    {tentar && <button className={secundario} type="button" onClick={tentar}>Tentar novamente</button>}
  </div>;
}

function veiculo(achado: Achado) {
  return `${achado.veiculoMarca} ${achado.veiculoModelo} · ${achado.veiculoPlaca}`;
}

export function AchadosPerdidosPage() {
  const { base } = usePortal();
  const location = useLocation();
  const [items, setItems] = useState<Achado[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filtro, setFiltro] = useState<"TODOS" | "EM_GUARDA" | "DEVOLVIDO">("TODOS");
  const [busca, setBusca] = useState("");
  const [sending, setSending] = useState<number | null>(null);
  const [notice, setNotice] = useState(() => (location.state as { notice?: string } | null)?.notice ?? "");

  const carregar = useCallback(async () => {
    setLoading(true); setError("");
    try { setItems(await listarAchados()); }
    catch (failure) { setError(mensagem(failure)); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => {
    let ativo = true;
    listarAchados().then(resultado => { if (ativo) setItems(resultado); })
      .catch(failure => { if (ativo) setError(mensagem(failure)); })
      .finally(() => { if (ativo) setLoading(false); });
    return () => { ativo = false; };
  }, []);

  const filtrados = items.filter(item => {
    const emGuarda = item.status === "EM_GUARDA" || item.status === "EM_CUSTODIA";
    const statusOk = filtro === "TODOS" || (filtro === "EM_GUARDA" ? emGuarda : item.status === filtro);
    const termo = busca.trim().toLocaleLowerCase("pt-BR");
    return statusOk && (!termo || `${item.descricao} ${veiculo(item)} ${item.freteId ?? ""} ${item.id}`.toLocaleLowerCase("pt-BR").includes(termo));
  });

  async function confirmar(item: Achado) {
    setSending(item.id); setNotice("");
    try {
      await confirmarDevolucao(item.id);
      setItems(current => current.map(record => record.id === item.id ? { ...record, status: "DEVOLVIDO" } : record));
      setNotice("Situação atualizada para devolvido.");
    } catch (failure) { setError(mensagem(failure)); }
    finally { setSending(null); }
  }

  return <Layout title="Achados e perdidos">
    <div className="grid gap-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-[11px] font-semibold text-[#17695b]">RF015 / MUV</p>
          <h1 className="mt-2 flex items-center gap-3 text-2xl font-bold tracking-[-0.02em] sm:text-[30px]">
            <span className="grid h-12 w-12 place-items-center rounded-xl bg-[#17695b] text-white"><LuArchive size={23} /></span>
            Achados e perdidos
          </h1>
          <p className="mt-2 max-w-2xl text-sm text-[#637479]">Consulte objetos encontrados nos veículos e acompanhe a situação da devolução.</p>
        </div>
        <Link className={primario} to={`${base}/novo`}><LuPlus size={17} /> Registrar objeto encontrado</Link>
      </div>

      <section className={`${painel} grid gap-4`} aria-label="Busca e filtros">
        <label className="relative block">
          <LuSearch className="absolute top-1/2 left-3 -translate-y-1/2 text-[#637479]" size={17} />
          <span className="sr-only">Buscar por descrição, veículo, frete ou registro</span>
          <input className={`${campo} mt-0 pl-10`} value={busca} onChange={event => setBusca(event.target.value)} placeholder="Buscar por item, veículo ou registro" />
        </label>
        <div className="flex flex-wrap gap-2" aria-label="Filtrar situação">
          {(["TODOS", "EM_GUARDA", "DEVOLVIDO"] as const).map(status => <button key={status} type="button" onClick={() => setFiltro(status)} aria-pressed={filtro === status}
            className={`min-h-9 rounded-lg px-3 text-xs font-semibold ${filtro === status ? "bg-[#17695b] text-white" : "bg-[#eff3f4] text-[#637479] hover:bg-[#e5f2ee]"}`}>
            {status === "TODOS" ? `Todos (${items.length})` : status === "EM_GUARDA" ? `Em guarda (${items.filter(item => item.status === "EM_GUARDA" || item.status === "EM_CUSTODIA").length})` : `Devolvidos (${items.filter(item => item.status === status).length})`}
          </button>)}
        </div>
      </section>

      {notice && <p role="status" className="rounded-lg bg-[#e5f2ee] px-4 py-3 text-sm font-semibold text-[#17695b]">{notice}</p>}
      {error ? <Estado texto={error} erro tentar={() => void carregar()} /> : loading ? <Estado texto="Carregando achados e perdidos…" />
        : filtrados.length === 0 ? <Estado texto={items.length === 0 ? "Ainda não há objetos registrados para os veículos vinculados a esta conta." : "Nenhum objeto corresponde à busca e ao filtro selecionados."} />
          : <div className="grid gap-4">{filtrados.map(item => <article key={item.id} className={`${painel} grid gap-4 md:grid-cols-[192px_minmax(0,1fr)]`}>
            {item.foto ? <img src={item.foto} alt={`Foto do objeto: ${item.descricao}`} className="h-48 w-full rounded-lg bg-[#eff3f4] object-cover md:h-full md:min-h-40" />
              : <div aria-label="Foto indisponível" className="grid min-h-40 place-items-center rounded-lg bg-[#eff3f4] text-sm text-[#637479]">Foto indisponível</div>}
            <div className="flex min-w-0 flex-col gap-4">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div><p className="text-xs text-[#637479]">Registro #{item.id}</p><h2 className="mt-1 text-lg font-semibold text-[#1d2d32]">{item.descricao}</h2></div>
                <span className={`rounded-full px-3 py-1 text-xs font-semibold ${item.status === "DEVOLVIDO" ? "bg-[#d4e8e0] text-[#17695b]" : "bg-[#ffe0d3] text-[#8b3d19]"}`}>
                  {item.status === "DEVOLVIDO" ? "Devolvido" : item.status === "EM_GUARDA" || item.status === "EM_CUSTODIA" ? "Em guarda" : item.status}
                </span>
              </div>
              <dl className="grid gap-3 rounded-lg bg-[#f5f7f8] p-4 text-sm sm:grid-cols-2">
                <div><dt className="text-xs text-[#637479]">Veículo</dt><dd className="mt-1 font-semibold">{veiculo(item)}</dd></div>
                <div><dt className="text-xs text-[#637479]">Data do encontro</dt><dd className="mt-1 font-semibold">{dataFormatada.format(new Date(item.dataEncontrado))}</dd></div>
                {item.freteId !== null && <div><dt className="text-xs text-[#637479]">Frete relacionado</dt><dd className="mt-1 font-semibold">Frete #{item.freteId}</dd></div>}
              </dl>
              {(item.status === "EM_GUARDA" || item.status === "EM_CUSTODIA") && <div className="flex flex-wrap justify-end">
                <button className={primario} type="button" disabled={sending !== null} onClick={() => void confirmar(item)}>
                  <LuCheck size={17} /> {sending === item.id ? "Atualizando…" : "Confirmar devolução"}
                </button>
              </div>}
            </div>
          </article>)}</div>}
    </div>
  </Layout>;
}

export function RegistrarAchadoPage() {
  const { base } = usePortal();
  const navigate = useNavigate();
  const [opcoes, setOpcoes] = useState<OpcoesAchado | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [descricao, setDescricao] = useState("");
  const [data, setData] = useState(() => {
    const hoje = new Date();
    return `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, "0")}-${String(hoje.getDate()).padStart(2, "0")}`;
  });
  const [hora, setHora] = useState(() => new Date().toTimeString().slice(0, 5));
  const [veiculoId, setVeiculoId] = useState("");
  const [freteId, setFreteId] = useState("");
  const [foto, setFoto] = useState("");
  const [fotoErro, setFotoErro] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState("");

  const carregarOpcoes = useCallback(async () => {
    setLoading(true); setLoadError("");
    try { setOpcoes(await consultarOpcoesAchado()); }
    catch (failure) { setLoadError(mensagem(failure)); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => {
    let ativo = true;
    consultarOpcoesAchado().then(resultado => { if (ativo) setOpcoes(resultado); })
      .catch(failure => { if (ativo) setLoadError(mensagem(failure)); })
      .finally(() => { if (ativo) setLoading(false); });
    return () => { ativo = false; };
  }, []);

  const veiculoSelecionado = opcoes?.veiculos.find(item => String(item.id) === veiculoId);
  const fretesDisponiveis = opcoes?.fretes.filter(item => !veiculoSelecionado || item.prestadorId === veiculoSelecionado.prestadorId) ?? [];

  async function selecionarFoto(event: ChangeEvent<HTMLInputElement>) {
    setFotoErro(""); setFoto("");
    const file = event.target.files?.[0];
    if (!file) return;
    if (!["image/png", "image/jpeg", "image/gif"].includes(file.type)) { setFotoErro("Use uma imagem PNG, JPEG ou GIF."); return; }
    if (file.size > 1_048_576) { setFotoErro("A foto deve ter no máximo 1 MB."); return; }
    const reader = new FileReader();
    reader.onload = () => setFoto(typeof reader.result === "string" ? reader.result : "");
    reader.onerror = () => setFotoErro("Não foi possível carregar a foto selecionada.");
    reader.readAsDataURL(file);
  }

  async function enviar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError("");
    if (!veiculoId || !foto || !data || !hora || !descricao.trim()) return;
    setEnviando(true);
    try {
      await registrarAchado({ descricao: descricao.trim(), dataEncontrado: `${data}T${hora}:00`,
        veiculoId: Number(veiculoId), freteId: freteId ? Number(freteId) : null, foto });
      navigate(base, { state: { notice: "Objeto registrado e disponível na situação em guarda." } });
    } catch (failure) { setError(mensagem(failure)); }
    finally { setEnviando(false); }
  }

  return <Layout title="Registrar objeto encontrado">
    <div className="grid gap-6">
      <div>
        <p className="text-[11px] font-semibold text-[#17695b]">RF015 / MUV</p>
        <h1 className="mt-2 text-2xl font-bold tracking-[-0.02em] sm:text-[30px]">Encontrou um objeto no veículo?</h1>
        <p className="mt-2 text-sm text-[#637479]">Registre uma foto, a descrição, a data e o veículo para acompanhar a devolução.</p>
      </div>
      {loading ? <Estado texto="Carregando veículos disponíveis…" /> : loadError ? <Estado texto={loadError} erro tentar={() => void carregarOpcoes()} />
        : <form onSubmit={enviar} className="grid items-start gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(320px,.9fr)]">
          <div className="grid gap-5">
            <section className={painel}>
              <h2 className="text-lg font-semibold">Foto do objeto <span aria-hidden="true">*</span></h2>
              <label className="mt-4 flex min-h-48 cursor-pointer flex-col items-center justify-center gap-3 rounded-lg border border-dashed border-[#b8c8cb] bg-[#f8fafb] p-4 text-center text-sm text-[#637479] hover:border-[#17695b]">
                {foto ? <img src={foto} alt="Prévia do objeto selecionado" className="max-h-72 w-full rounded-lg object-contain" /> : <><LuPlus size={24} className="text-[#17695b]" /><span>Escolha uma foto PNG, JPEG ou GIF (até 1 MB)</span></>}
                <input className="sr-only" type="file" accept="image/png,image/jpeg,image/gif" onChange={event => void selecionarFoto(event)} />
              </label>
              {fotoErro && <p role="alert" className="mt-3 text-sm text-red-700">{fotoErro}</p>}
            </section>
            <section className={`${painel} grid gap-4`}>
              <h2 className="text-lg font-semibold">Dados do objeto</h2>
              <label className="text-xs font-semibold text-[#637479]">Descrição *
                <textarea className="mt-2 min-h-28 w-full resize-y rounded-lg border border-[#d8e2e5] bg-[#f8fafb] p-3 text-sm font-normal outline-none focus:border-[#17695b]" maxLength={255} required value={descricao} onChange={event => setDescricao(event.target.value)} placeholder="Descreva o objeto encontrado" />
                <span className="mt-1 block text-right font-normal">{descricao.length}/255</span>
              </label>
            </section>
          </div>

          <section className={`${painel} grid gap-4`}>
            <h2 className="text-lg font-semibold">Onde e quando foi encontrado?</h2>
            <label className="text-xs font-semibold text-[#637479]">Veículo *
              <select className={campo} required value={veiculoId} onChange={event => { setVeiculoId(event.target.value); setFreteId(""); }}>
                <option value="">Selecione um veículo</option>
                {opcoes?.veiculos.map(item => <option key={item.id} value={item.id}>{item.marca} {item.modelo} · {item.placa}</option>)}
              </select>
              {opcoes?.veiculos.length === 0 && <span className="mt-1 block font-normal">Não há veículos ativos disponíveis para esta conta.</span>}
            </label>
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="text-xs font-semibold text-[#637479]">Data *<input className={campo} type="date" required value={data} onChange={event => setData(event.target.value)} /></label>
              <label className="text-xs font-semibold text-[#637479]">Horário *<input className={campo} type="time" required value={hora} onChange={event => setHora(event.target.value)} /></label>
            </div>
            <label className="text-xs font-semibold text-[#637479]">Frete relacionado (opcional)
              <select className={campo} value={freteId} onChange={event => setFreteId(event.target.value)}>
                <option value="">Sem frete relacionado</option>
                {fretesDisponiveis.map(item => <option key={item.id} value={item.id}>Frete #{item.id} · {item.status}</option>)}
              </select>
            </label>
            <div className="rounded-xl bg-[#e5f2ee] p-4 text-sm">
              <p className="font-semibold text-[#17695b]">Situação inicial: Em guarda</p>
              <p className="mt-1 text-[#637479]">O registro ficará disponível ao prestador responsável e aos motoristas vinculados. A identificação e o contato com o proprietário ocorrem fora do MUV.</p>
            </div>
            {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
            <div className="flex flex-wrap gap-3">
              <button className={primario} type="submit" disabled={enviando || !foto || !opcoes?.veiculos.length || !veiculoId}>
                {enviando ? "Registrando…" : "Registrar objeto"}
              </button>
              <button className={secundario} type="button" disabled={enviando} onClick={() => navigate(base)}>Voltar</button>
            </div>
          </section>
        </form>}
    </div>
  </Layout>;
}
