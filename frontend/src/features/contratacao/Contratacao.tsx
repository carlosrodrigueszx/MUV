import { useEffect, useRef, useState } from "react";
import type { ReactNode } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { AccountNavbar } from "../freight-history/components/AccountNavbar";
import { aceitar, comparar, detalhar } from "./api";
import type { Solicitacao } from "./api";

const painel = "rounded-2xl border border-[#d8e2e5] bg-white p-6";
const destaque = "rounded-2xl bg-[#e2f1ed] p-4 text-sm";
const acao = "inline-flex cursor-pointer rounded-lg border px-4 py-3 text-sm font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#176659] disabled:cursor-not-allowed disabled:bg-[#e3e9eb] disabled:text-[#61747c]";
const botao = acao + " border-[#d8e2e5] text-[#176659] not-disabled:hover:border-[#176659] not-disabled:hover:bg-[#e2f1ed]";
const primario = acao + " border-[#176659] bg-[#176659] text-white not-disabled:hover:border-[#0e4940] not-disabled:hover:bg-[#0e4940] disabled:border-transparent";
const moeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const data = new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium" });
const dataHora = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

function Layout({ children }: { children: ReactNode }) {
  return <>
    <AccountNavbar />
    <main className="min-h-screen bg-[#f5f7f8] pt-20 text-[#1b2c32]">
      <div className="mx-auto grid max-w-[1440px] gap-6 px-5 py-9 sm:px-8 lg:px-12">{children}</div>
    </main>
  </>;
}

function useConsulta<T>(id: string, consultar: (id: string, signal: AbortSignal) => Promise<T>) {
  const [resultado, setResultado] = useState<{ id: string; dados?: T; erro?: string }>();
  useEffect(() => {
    const controle = new AbortController();
    consultar(id, controle.signal).then(dados => {
      if (!controle.signal.aborted) setResultado({ id, dados });
    }).catch((erro: Error) => {
      if (!controle.signal.aborted) setResultado({ id, erro: erro.message });
    });
    return () => controle.abort();
  }, [id, consultar]);
  return resultado?.id === id ? resultado : undefined;
}

function Estado({ erro }: { erro?: string }) {
  return <Layout>{erro ? <p role="alert">{erro}</p> : <p role="status">Carregando…</p>}</Layout>;
}

function trecho(solicitacao: Solicitacao) {
  return `${solicitacao.origem} → ${solicitacao.destino}`;
}

export function CompararPropostas() {
  const { id = "" } = useParams();
  return <Comparacao key={id} id={id} />;
}

function Comparacao({ id }: { id: string }) {
  const resultado = useConsulta(id, comparar);
  const navegar = useNavigate();
  const envio = useRef(false);
  const revisao = useRef<HTMLElement>(null);
  const [selecionada, selecionar] = useState<number>();
  const [concordou, concordar] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState("");
  if (!resultado?.dados) return <Estado erro={resultado?.erro} />;
  const { solicitacao, propostas, freteId } = resultado.dados;
  const proposta = propostas.find(item => item.id === selecionada);
  const aberta = solicitacao.status === "ABERTA";

  async function confirmar() {
    if (!proposta || !concordou || envio.current) return;
    envio.current = true;
    setEnviando(true);
    setErro("");
    try {
      const frete = await aceitar(id, proposta.id);
      navegar(`/cliente/fretes/${frete.id}`);
    } catch (falha) {
      setErro(falha instanceof Error ? falha.message : "Não foi possível confirmar a contratação.");
    } finally {
      envio.current = false;
      setEnviando(false);
    }
  }

  return <Layout>
    <h1 className="text-3xl font-bold">Compare as propostas recebidas</h1>
    <p className="text-sm text-[#61747c]">FR-{id} · {aberta ? "Solicitação aberta" : "Solicitação encerrada"} · {solicitacao.modalidade === "ENCOMENDA" ? "Encomendas" : "Pessoas"} · {trecho(solicitacao)} · {data.format(new Date(solicitacao.dataPretendida))}</p>
    {aberta && <div className={destaque}>
      <p className="font-semibold text-[#124d43]">Escolha apenas uma proposta</p>
      <p className="mt-1">Ao confirmar a contratação, as demais propostas serão encerradas para esta solicitação.</p>
    </div>}
    {freteId !== null && <Link className={botao + " w-fit"} to={`/cliente/fretes/${freteId}`}>Ver frete contratado</Link>}
    {propostas.length === 0 && <p>Ainda não há propostas para comparar.</p>}
    <div className="grid items-start gap-5 md:grid-cols-2 xl:grid-cols-3">
      {propostas.map(item => {
        const escolhida = item.id === selecionada;
        const disponivel = aberta && item.status === "ENVIADA";
        return <article className={painel + " grid gap-4"} key={item.id}>
          <h2 className="text-xl font-semibold">{item.prestador}</h2>
          <p className="text-3xl font-bold text-[#176659]">{moeda.format(item.valor)}</p>
          {item.autorizado && <p className="text-xs font-semibold text-[#176659]">Autorizado para {solicitacao.modalidade === "ENCOMENDA" ? "encomendas" : "transporte de pessoas"}</p>}
          {item.tipoPrestador && <p className="text-sm">{item.tipoPrestador === "AUTONOMO" ? "Prestador autônomo" : "Empresa ou cooperativa"}{item.avaliacao !== null && ` · Avaliação ${item.avaliacao.toLocaleString("pt-BR", { maximumFractionDigits: 2 })}/5`}</p>}
          <p className="whitespace-pre-line text-sm">{item.condicoes}</p>
          <p className="text-xs font-semibold text-[#61747c]">{escolhida ? "Selecionada para revisão" : disponivel ? "Disponível" : item.status === "ACEITA" ? "Aceita" : "Encerrada"}</p>
          {disponivel && <button type="button" className={(escolhida ? primario : botao) + " w-fit"} disabled={enviando} onClick={() => {
            if (item.id !== selecionada) concordar(false);
            selecionar(item.id);
            setErro("");
            requestAnimationFrame(() => revisao.current?.focus());
          }}>{escolhida ? "Revisar contratação" : "Ver proposta"}</button>}
        </article>;
      })}
    </div>
    {proposta && aberta && <section ref={revisao} tabIndex={-1} className={painel + " grid scroll-mt-24 gap-4"} aria-labelledby="titulo-revisao">
      <h2 id="titulo-revisao" className="text-xl font-semibold">Revisão antes do aceite</h2>
      <div className="text-sm">
        <p>Prestador: {proposta.prestador} · Proposta P-{proposta.id}</p>
        <p>Valor acordado: {moeda.format(proposta.valor)}</p>
        <p className="whitespace-pre-line">{proposta.condicoes}</p>
      </div>
      <label className="flex items-center gap-2 text-sm font-semibold">
        <input type="checkbox" checked={concordou} disabled={enviando} onChange={event => concordar(event.target.checked)} />
        Li e aceito o valor e as condições desta proposta.
      </label>
      {erro && <p role="alert" className="text-sm text-red-700">{erro}</p>}
      <button type="button" disabled={!concordou || enviando} className={primario + " w-fit"} onClick={confirmar}>
        {enviando ? "Confirmando…" : `Confirmar contratação de ${proposta.prestador}`}
      </button>
      <p className="text-xs text-[#61747c]">Se outra proposta já tiver sido aceita, um novo aceite será bloqueado. Nenhum pagamento é realizado nesta etapa.</p>
    </section>}
  </Layout>;
}

const situacoes: Record<string, string> = {
  AGUARDANDO_DESIGNACAO: "Aguardando designação",
  PRONTO_INICIO: "Pronto para início",
  EM_EXECUCAO: "Em execução",
  INTERROMPIDO: "Interrompido",
  CONCLUIDO: "Concluído",
  CANCELADO: "Cancelado",
  ENCERRADO_SEM_CONCLUSAO: "Encerrado sem conclusão",
};

export function FreteContratado() {
  const { id = "" } = useParams();
  const resultado = useConsulta(id, detalhar);
  if (!resultado?.dados) return <Estado erro={resultado?.erro} />;
  const { solicitacao, frete, outrasPropostas } = resultado.dados;
  const historico = `/cliente/historico?frete=${frete.id}&periodo=${frete.contratadoEm.slice(0, 7)}`;
  const campos = [
    ["Prestador responsável", `${frete.prestador} · Proposta P-${frete.propostaId}`],
    ["Valor acordado", moeda.format(frete.valor)],
    [solicitacao.modalidade === "ENCOMENDA" ? "Coleta prevista" : "Embarque previsto", dataHora.format(new Date(solicitacao.dataPretendida))],
    ["Condições", frete.condicoes],
  ];
  return <Layout>
    <h1 className="text-3xl font-bold">Frete contratado. Tudo registrado.</h1>
    <p className="text-sm text-[#61747c]">FR-{solicitacao.id} · Contratação confirmada em {dataHora.format(new Date(frete.contratadoEm))}</p>
    <div className={destaque}>
      <p className="font-semibold text-[#124d43]">Uma única proposta aceita</p>
      <p className="mt-1">{frete.prestador} é o prestador responsável.{outrasPropostas.length > 0 && " As demais propostas foram encerradas."}</p>
    </div>
    <div className="grid items-start gap-5 lg:grid-cols-2">
      <section className={painel}>
        <h2 className="text-xl font-semibold">Condições acordadas</h2>
        <p className="my-4 text-2xl font-bold">{trecho(solicitacao)}</p>
        <dl className="grid gap-4">
          {campos.map(([rotulo, valor]) => valor && <div key={rotulo}>
            <dt className="mb-1.5 text-xs font-semibold text-[#61747c]">{rotulo}</dt>
            <dd className="whitespace-pre-line rounded-lg border border-[#d8e2e5] bg-[#f5f7f8] p-3 text-sm">{valor}</dd>
          </div>)}
        </dl>
      </section>
      <div className="grid gap-4">
        <section className={painel + " grid gap-4"}>
          <h2 className="text-xl font-semibold">O que acontece agora?</h2>
          <div className="rounded-2xl bg-[#fff3de] p-4 text-sm">
            <p className="font-semibold text-[#925705]">{situacoes[frete.status] ?? frete.status}</p>
            {frete.status === "AGUARDANDO_DESIGNACAO" && <p className="mt-1">O prestador deverá designar um motorista e um veículo compatíveis antes de iniciar o serviço.</p>}
          </div>
          <Link className={primario + " w-fit"} to={historico}>Acompanhar frete</Link>
          <Link className={botao + " w-fit"} to={historico + "#freight-details"}>Ver histórico da solicitação</Link>
        </section>
        {outrasPropostas.length > 0 && <section className={painel + " grid gap-4"}>
          <h2 className="text-xl font-semibold">Outras propostas</h2>
          <ul className="text-sm">{outrasPropostas.map(item => <li key={item.id}>{item.prestador} · P-{item.id} — Encerrada</li>)}</ul>
          <button type="button" disabled className={botao + " w-fit"}>Aceitar outra proposta</button>
        </section>}
      </div>
    </div>
  </Layout>;
}
