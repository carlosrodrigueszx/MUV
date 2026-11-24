import { lazy, Suspense, useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { LuCheck, LuSend, LuShieldCheck, LuX } from "react-icons/lu";
import { ProviderLayout } from "../../components/layout/ProviderLayout";
import { ConflitoProposta, avaliar, enviar, obterPercurso, type Rota, type Avaliacao } from "./api";
import { getSession } from "../auth/api";
import { duracao } from "./duracao";
import "./propostas.css";

const Percurso = lazy(() => import("./Percurso").then(modulo => ({ default: modulo.Percurso })));

const reais = (valor: number) =>
  valor.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
const horario = (data: string) =>
  new Date(data).toLocaleTimeString("pt-BR", {
    hour: "2-digit",
    minute: "2-digit",
  });
type Acao = "MANTER" | "AJUSTAR" | "RECUSAR";

export default function PaginaAvaliacao() {
  const { id = "" } = useParams();
  return <Detalhe key={id} id={id} />;
}

function Detalhe({ id }: { id: string }) {
  const [dados, setDados] = useState<Avaliacao>();
  const [rota, setRota] = useState<Rota | null>();
  const [prestador, setPrestador] = useState("");
  const [erro, setErro] = useState("");

  const [acao, setAcao] = useState<Acao>("AJUSTAR");
  const [preco, setPreco] = useState("");
  const [mensagem, setMensagem] = useState("");
  const [ocupado, setOcupado] = useState(false);
  const trava = useRef(false);
  const tentativa = useRef({ corpo: "", chave: "" });
  const [modalAberto, setModalAberto] = useState(false);
  const modal = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const controller = new AbortController();
    avaliar(id, controller.signal)
      .then(detalhe => {
        setDados(detalhe);
        obterPercurso(id, controller.signal).then(setRota).catch(() => {
          if (!controller.signal.aborted) setRota(null);
        });
      })
      .catch((e) => {
        if (!controller.signal.aborted) setErro(e.message);
      });
    getSession(controller.signal)
      .then((s) => setPrestador(s?.nome ?? ""))
      .catch(() => {
        if (!controller.signal.aborted) setErro("Não foi possível carregar o perfil do prestador.");
      });
    return () => controller.abort();
  }, [id]);

  function abrir(tipo: Acao) {
    setAcao(tipo);
    setMensagem("");
    setPreco("");
    setErro("");

    setModalAberto(true);
    modal.current?.showModal();
  }
  const numero = /^\d+(?:[.,]\d{1,2})?$/.test(preco)
    ? Number(preco.replace(",", "."))
    : NaN;
  async function confirmar(tipo: Acao) {
    if (!dados || trava.current) return;
    setErro("");

    if (tipo !== "MANTER" && !mensagem.trim()) {
      setErro("Informe a justificativa.");
      return;
    }
    if (tipo === "AJUSTAR" && !(numero > 0)) {
      setErro("Informe um valor positivo com até duas casas decimais.");
      return;
    }
    const corpo = {
      tipo,
      valorRevisado: dados.valorCliente,
      condicoesRevisadas: dados.condicoesCliente,
      novoValor: tipo === "AJUSTAR" ? numero : null,
      justificativa: tipo === "MANTER" ? null : mensagem.trim(),
    };
    const serializado = JSON.stringify(corpo);
    if (tentativa.current.corpo !== serializado)
      tentativa.current = { corpo: serializado, chave: crypto.randomUUID() };
    trava.current = true;
    setOcupado(true);
    try {
      await enviar(
        id,
        tipo === "RECUSAR",
        tipo === "RECUSAR"
          ? { justificativa: mensagem.trim() }
          : { ...corpo, chaveEnvio: tentativa.current.chave },
      );
      if (tipo === "RECUSAR")
        setDados({ ...dados, justificativaRecusa: mensagem.trim() });
      else setDados({ ...dados, propostaEnviada: true });
      window.dispatchEvent(new Event("muv:solicitacao-respondida"));
      modal.current?.close();
    } catch (e) {
      if (e instanceof ConflitoProposta) {
        const atual = await avaliar(id).catch(() => undefined);
        if (atual?.propostaEnviada) {
          setDados(atual);
          modal.current?.close();
          return;
        }
      }
      setErro(e instanceof Error ? e.message : "Falha no envio.");
    } finally {
      trava.current = false;
      setOcupado(false);
    }
  }

  const pessoa = dados?.modalidade === "PESSOA";
  const bloqueado = ocupado || dados?.status !== "ABERTA";
  const diferenca =
    dados?.valorCliente != null && Number.isFinite(numero)
      ? numero - dados.valorCliente
      : null;
  return (
    <div className="rf007">
      <ProviderLayout
        pageTitle="Solicitações"
        activeNavKey="requests"
        organizationName={prestador}
        profileLabel="Prestador"
        isOnline={null}
      >
        {erro && !modalAberto && <p role="alert">{erro}</p>}
        {!dados && !erro && <p role="status">Carregando solicitação…</p>}
        {dados && (
          <>
            <small>Solicitações / {dados.id} / Avaliar itinerário</small>
            <h1>
              {dados.id} · {pessoa ? "Pessoas" : "Encomendas"}
            </h1>
            <p>
              Revise o trajeto e os detalhes antes de responder a{" "}
              {dados.cliente}.
            </p>
            <div className="rf-grade">
              <div>
                <section>
                  <div className="rf-pessoa">
                    <span aria-hidden="true">
                      {dados.cliente
                        .split(/\s+/)
                        .filter(Boolean)
                        .slice(0, 2)
                        .map((s) => s[0])
                        .join("")}
                    </span>
                    <strong>{dados.cliente}</strong>
                  </div>
                  <hr />
                  <div className="rf-linha">
                    <div>
                      <small>DATA DA VIAGEM</small>
                      <b>
                        {new Date(dados.partida).toLocaleDateString("pt-BR")}
                      </b>
                      {dados.retornoPrevisto && <small>
                        Retorno: {new Date(dados.retornoPrevisto).toLocaleString("pt-BR")}
                      </small>}
                    </div>
                    <div>
                      <small>{pessoa ? "GRUPO E MODALIDADE" : "VOLUMES"}</small>
                      <b>
                        {pessoa
                          ? `${dados.passageiros} ${dados.passageiros === 1 ? "pessoa" : "pessoas"}`
                          : dados.volumes}
                      </b>
                    </div>
                  </div>
                </section>
                <section>
                  <h2>O caminho planejado</h2>
                  <ol className="rf-itinerario">
                    {(rota?.pontos ?? [
                      { endereco: dados.origem, horario: dados.partida },
                      ...dados.paradas,
                      { endereco: dados.destino, horario: null },
                    ]).map((p, i) => (
                      <li key={i}>
                        {p.horario ? <time dateTime={p.horario}>
                          {horario(p.horario)}
                          {p.horario.slice(0, 10) !== dados.partida.slice(0, 10) &&
                            <small>{new Date(p.horario).toLocaleDateString("pt-BR")}</small>}
                        </time> : <small>{rota === undefined ? "Calculando…" : "Sem previsão"}</small>}
                        <i aria-hidden="true" />
                        <strong>{p.endereco}
                          {rota && !rota.pontos[i].coordenada && <small className="block">Localização indisponível</small>}
                        </strong>
                      </li>
                    ))}
                  </ol>
                  {pessoa && dados.bagagens && (
                    <p>Bagagens · {dados.bagagens}</p>
                  )}
                  {!pessoa && (
                    <>
                      <p>Peso · {dados.pesoKg} kg</p>
                      {dados.coleta && <p>Coleta · {dados.coleta}</p>}
                      {dados.entrega && <p>Entrega · {dados.entrega}</p>}
                    </>
                  )}
                </section>
              </div>
              <div>
                <Suspense fallback={<section className="rf-mapa">Carregando mapa…</section>}>
                  <Percurso dados={dados} rota={rota} />
                </Suspense>
                <section>
                  <small>Proposta do cliente</small>
                  <h2 className="rf-preco">
                    {dados.valorCliente != null
                      ? reais(dados.valorCliente)
                      : "Valor indisponível"}
                  </h2>
                  <hr />
                  <div className="rf-metricas">
                    {dados.percursoKm != null && (
                      <div>
                        <b>{dados.percursoKm.toLocaleString("pt-BR")} km</b>
                        <small>Percurso total</small>
                      </div>
                    )}
                    {dados.deslocamentoMinutos != null && (
                      <div>
                        <b>{duracao(dados.deslocamentoMinutos)}</b>
                        <small>Deslocamento</small>
                      </div>
                    )}
                    {dados.duracaoMinutos != null && (
                      <div>
                        <b>{duracao(dados.duracaoMinutos)}</b>
                        <small>Saída até a chegada</small>
                      </div>
                    )}
                  </div>
                  <p>{dados.condicoesCliente}</p>
                  <aside>
                    <LuShieldCheck /> Considere a espera e os pedidos especiais.
                  </aside>
                </section>
              </div>
            </div>
            {dados.propostaEnviada ? <p className="rf-barra" role="status">Proposta enviada ao cliente.</p> : <section className="rf-barra">
              <p>Sua resposta será enviada a {dados.cliente}.</p>
              <div className="rf-acoes">
                <button
                  className="rf-recusar"
                  disabled={bloqueado}
                  onClick={() => abrir("RECUSAR")}
                >
                  Recusar
                </button>
                <button disabled={bloqueado} onClick={() => abrir("AJUSTAR")}>
                  Fazer contraproposta
                </button>
                <button
                  className="rf-principal"
                  disabled={
                    bloqueado ||
                    dados.valorCliente == null ||
                    !dados.condicoesCliente?.trim()
                  }
                  onClick={() => confirmar("MANTER")}
                >
                  <LuCheck /> Aceitar
                  {dados.valorCliente != null &&
                    " por " + reais(dados.valorCliente)}
                </button>
              </div>
            </section>
            }
            {dados.status !== "ABERTA" && (
              <p role="status">Solicitação encerrada.</p>
            )}
            {!dados.propostaEnviada && dados.justificativaRecusa && (
              <p role="status">
                Proposta recusada · {dados.justificativaRecusa}
              </p>
            )}

            <dialog
              ref={modal}
              aria-labelledby="rf-titulo"
              onCancel={(e) => {
                if (ocupado) e.preventDefault();
              }}
              onClose={() => {
                setModalAberto(false);
                setErro("");
              }}
            >
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  void confirmar(acao);
                }}
              >
                <div className="rf-linha">
                  <small>SOLICITAÇÃO {dados.id}</small>
                  <button
                    type="button"
                    aria-label="Fechar"
                    disabled={ocupado}
                    onClick={() => modal.current?.close()}
                  >
                    <LuX />
                  </button>
                </div>
                <h2 id="rf-titulo">
                  {acao === "RECUSAR"
                    ? "Recusar proposta"
                    : "Vamos combinar um novo valor"}
                </h2>
                <p>
                  {acao === "RECUSAR"
                    ? "Explique o motivo da recusa a "
                    : "Envie uma proposta clara para "}
                  {dados.cliente}.<br />
                  {dados.origem} → {dados.destino} ·{" "}
                  {new Date(dados.partida).toLocaleDateString("pt-BR")}
                </p>
                {acao === "AJUSTAR" && (
                  <>
                    <div className="rf-oferta">
                      <small>Proposta do cliente</small>
                      <strong>
                        {dados.valorCliente == null
                          ? "Indisponível"
                          : reais(dados.valorCliente)}
                      </strong>
                    </div>
                    <label>
                      Novo valor do fretamento *
                      <input
                        inputMode="decimal"
                        required
                        value={preco}
                        onChange={(e) => setPreco(e.target.value)}
                      />
                    </label>
                    <small>
                      Inclua combustível, pedágios e seus adicionais.
                    </small>
                  </>
                )}
                <label>
                  Justificativa para o cliente *
                  <textarea
                    required
                    maxLength={acao === "AJUSTAR" ? 500 : undefined}
                    value={mensagem}
                    onChange={(e) => setMensagem(e.target.value)}
                  />
                </label>
                {acao === "AJUSTAR" && (
                  <>
                    <small>{mensagem.length}/500</small>
                    {diferenca != null && (
                      <div className="rf-linha">
                        <small>Ajuste em relação à proposta do cliente</small>
                        <strong>
                          {diferenca > 0 ? "+ " : ""}{reais(diferenca)} ·{" "}
                          {(
                            (diferenca / dados.valorCliente!) *
                            100
                          ).toLocaleString("pt-BR", {
                            maximumFractionDigits: 1,
                          })}
                          %
                        </strong>
                      </div>
                    )}
                    <aside>
                      <LuShieldCheck /><div><strong>O itinerário não será alterado</strong><p>O cliente poderá avaliar sua proposta antes de confirmar a contratação.</p></div>
                    </aside>
                  </>
                )}
                {erro && <p role="alert">{erro}</p>}
                <div className="rf-acoes">
                  <button
                    type="button"
                    disabled={ocupado}
                    onClick={() => modal.current?.close()}
                  >
                    Voltar à avaliação
                  </button>
                  <button disabled={ocupado} className="rf-principal">
                    <LuSend />
                    {acao === "RECUSAR"
                      ? "Confirmar recusa"
                      : "Enviar contraproposta"}
                  </button>
                </div>
              </form>
            </dialog>
          </>
        )}
        {!dados && erro && <Link to="/conta">Voltar à conta</Link>}
      </ProviderLayout>
    </div>
  );
}
