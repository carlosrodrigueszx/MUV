import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { ProviderLayout } from "../../components/layout/ProviderLayout";

export type Item = {
  id: number; modalidade: "PESSOA" | "ENCOMENDA"; partida: string;
  origemCidade: string; origemEstado: string; destinoCidade: string; destinoEstado: string;
  passageiros: number | null; pesoKg: number | null; volumes: string | null; volumeM3: number | null;
  paradas: number; direcionada: boolean; autorizada: boolean;
};
type Perfil = { nome: string; tipo: string; modalidades: string[]; regioes: string[] };
const endpoint = "/api/prestador/solicitacoes";
const campo = "mt-2 block h-[46px] w-full min-w-0 rounded-lg border border-[#dfe6e7] bg-[#f5f7f8] px-3 text-sm font-normal text-[#1d2d32]";
const botao = "inline-flex min-h-10 items-center justify-center rounded-lg bg-[#17695b] px-[18px] text-[13px] font-semibold text-white hover:bg-[#12584c]";
const dia = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "short" });
const hora = new Intl.DateTimeFormat("pt-BR", { hour: "2-digit", minute: "2-digit" });

async function consultar<T>(url: string, signal: AbortSignal): Promise<T> {
  const resposta = await fetch(url, { signal, cache: "no-store" });
  if (!resposta.ok) throw new Error(resposta.status === 401 ? "Entre para consultar solicitações." : "Não foi possível carregar os dados do prestador.");
  return resposta.json();
}

function tituloAutorizacoes(modalidades: string[]) {
  const pessoas = modalidades.includes("PESSOA");
  const encomendas = modalidades.includes("ENCOMENDA");
  if (pessoas && encomendas) return "Pessoas e encomendas · Autorizadas";
  if (pessoas) return "Pessoas · Autorizada";
  if (encomendas) return "Encomendas · Autorizada";
  return "Nenhuma modalidade autorizada";
}

function Cartao({ item, nome }: { item: Item; nome?: string }) {
  const pessoas = item.modalidade === "PESSOA";
  const partida = new Date(item.partida);
  const resumo = [dia.format(partida).replace(" de ", " "), hora.format(partida)];
  if (pessoas && item.passageiros != null) resumo.push(`${item.passageiros} ${item.passageiros === 1 ? "passageiro" : "passageiros"}`);
  if (!pessoas && item.pesoKg != null) resumo.push(`${item.pesoKg.toLocaleString("pt-BR")} kg`);
  if (!pessoas && item.volumeM3 != null) resumo.push(`${item.volumeM3.toLocaleString("pt-BR", { maximumSignificantDigits: 6 })} m³`);
  else if (!pessoas && item.volumes) resumo.push(item.volumes);
  const status = [item.direcionada ? `Direcionada a ${nome || "você"}` : "Aberta"];
  if (item.paradas > 0) status.push(`${item.paradas} ${item.paradas === 1 ? "parada" : "paradas"}`);
  return <article className="flex flex-col gap-4 rounded-[14px] border border-[#dfe6e7] bg-white p-6 shadow-[0_4px_8px_#142d2519] lg:flex-row lg:items-center lg:justify-between">
    <div className="flex min-w-0 flex-col gap-4 break-words">
      <h2 className="text-xl font-semibold">{item.id} · {pessoas ? "Pessoas" : "Encomendas"}</h2>
      <p className="text-[17px] font-semibold">{item.origemCidade} → {item.destinoCidade}</p>
      <p className="text-[13px] text-[#637479]">{resumo.join(" · ")}</p>
      <p className="text-[13px] font-semibold text-[#17695b]">{status.join(" · ")}</p>
    </div>
    {item.autorizada ? <Link className={botao + " shrink-0 cursor-pointer self-start lg:self-center"} to={`${item.id}/avaliacao`}>Ver e apresentar proposta</Link>
      : <span className="text-sm text-[#637479]">Modalidade não autorizada</span>}
  </article>;
}

export default function Disponiveis() {
  const [perfil, setPerfil] = useState<Perfil>();
  const [erroPerfil, setErroPerfil] = useState("");
  const [itens, setItens] = useState<Item[]>([]);
  const [modalidade, setModalidade] = useState("TODAS");
  const [regiao, setRegiao] = useState("");
  const [data, setData] = useState("");
  const [query, setQuery] = useState("modalidade=TODAS");
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [regioesDisponiveis, setRegioesDisponiveis] = useState<string[]>([]);

  useEffect(() => {
    const abort = new AbortController();
    consultar<Perfil>(endpoint + "/perfil", abort.signal).then(setPerfil).catch(error => {
      if (!abort.signal.aborted) setErroPerfil(error.message);
    });
    return () => abort.abort();
  }, []);

  useEffect(() => {
    const abort = new AbortController();
    consultar<Item[]>(endpoint + "?" + query, abort.signal).then(lista => {
      if (abort.signal.aborted) return;
      setItens(lista);
      if (query === "modalidade=TODAS") {
        setRegioesDisponiveis([...new Set(lista.map(i => i.origemCidade + "/" + i.origemEstado))].sort());
      }
      setErro("");
      setCarregando(false);
    }).catch(error => {
      if (!abort.signal.aborted) { setErro(error.message); setCarregando(false); }
    });
    return () => abort.abort();
  }, [query]);

  function filtrar(evento: FormEvent) {
    evento.preventDefault();
    const params = new URLSearchParams({ modalidade });
    const local = regiao.trim();
    if (/^[a-z]{2}$/i.test(local)) params.set("estado", local);
    else if (local) {
      const [cidade, uf] = local.split("/");
      params.set("cidade", cidade.trim());
      if (uf) params.set("estado", uf.trim());
    }
    if (data) params.set("data", data);
    if (query !== params.toString()) { setCarregando(true); setQuery(params.toString()); }
  }

  return <ProviderLayout passengerPortalHref="/cliente/planejar-viagem" pageTitle="Solicitações abertas" activeNavKey="requests" organizationName={perfil?.nome} profileLabel={perfil?.tipo ?? "Prestador"} isOnline={null}>
    <div className="space-y-6 text-[#1d2d32]">
      <h1 className="text-[30px] leading-[1.45] font-bold tracking-tight">Solicitações disponíveis na sua região</h1>
      <p className="text-sm text-[#637479]">Apresente propostas apenas nas modalidades para as quais você está autorizado.</p>
      {erroPerfil && <p role="alert">{erroPerfil}</p>}
      {perfil && <section aria-label="Perfil de atuação" className="space-y-1 rounded-[14px] bg-[#e5f2ee] p-4">
        <h2 className="text-sm font-semibold text-[#005447]">{tituloAutorizacoes(perfil.modalidades)}</h2>
        <p className="text-[13px]">{perfil.nome} · Região: {perfil.regioes.join(", ") || "Sem região cadastrada"}</p>
      </section>}
      <form onSubmit={filtrar} className="rounded-[14px] border border-[#dfe6e7] bg-white p-6">
        <h2 className="mb-4 text-xl font-semibold">Filtrar solicitações</h2>
        <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">
          <label className="min-w-0 text-xs font-semibold text-[#637479]">Modalidade
            <select className={campo} value={modalidade} onChange={e => setModalidade(e.target.value)}>
              <option value="TODAS">Todas</option><option value="PESSOA">Pessoas</option><option value="ENCOMENDA">Encomendas</option>
            </select>
          </label>
          <label className="min-w-0 text-xs font-semibold text-[#637479]">Região
            <input className={campo} list="regioes-rf006" value={regiao} onChange={e => setRegiao(e.target.value)} placeholder="Cidade ou UF" />
            <datalist id="regioes-rf006">{[...new Set([...regioesDisponiveis, ...(perfil?.regioes ?? [])])].map(r => <option key={r} value={r} />)}</datalist>
          </label>
          <label className="min-w-0 text-xs font-semibold text-[#637479]">Data do serviço
            <input className={campo} type="date" value={data} onChange={e => setData(e.target.value)} />
          </label>
          <button className={botao + " h-[46px] self-end"}>Aplicar filtros</button>
        </div>
      </form>
      {erro && <p role="alert" className="text-red-700">{erro}</p>}
      {carregando && <p role="status">Carregando solicitações.</p>}
      {!carregando && !erro && (itens.length ? <div className="space-y-5">{itens.map(item => <Cartao key={item.id} item={item} nome={perfil?.nome} />)}</div>
        : <p role="status" className="rounded-[14px] border border-[#dfe6e7] bg-white p-6">Nenhuma solicitação encontrada para os filtros selecionados.</p>)}
    </div>
  </ProviderLayout>;
}