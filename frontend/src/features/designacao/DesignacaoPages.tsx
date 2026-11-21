import { useMemo, useState, type ReactNode } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ProviderLayout } from "../../components/layout/ProviderLayout";
import { DesignacaoApiError, designar, substituir } from "./api";
import type { ConsultaDesignacao, VeiculoDisponivel } from "./types";
import { useDesignacao, useEnvioUnico } from "./useDesignacao";

const painel = "rounded-xl border border-[#dfe6e7] bg-white p-5 sm:p-6";
const campo = "mt-2 h-11 w-full rounded-lg border border-[#d8e2e5] bg-[#f8fafb] px-3 text-sm outline-none focus:border-[#17695b]";
const botao = "inline-flex min-h-11 items-center justify-center rounded-lg border px-4 text-sm font-semibold focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#17695b] disabled:cursor-not-allowed disabled:opacity-55";
const primario = `${botao} border-[#17695b] bg-[#17695b] text-white hover:bg-[#104e43]`;
const secundario = `${botao} border-[#d8e2e5] bg-white text-[#17695b] hover:border-[#17695b]`;
const moeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const dataHora = new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium", timeStyle: "short" });

function Layout({ titulo, children }: { titulo: string; children: ReactNode }) {
  return <ProviderLayout
    pageTitle={titulo}
    activeNavKey="freights"
    organizationName=""
    isOnline={null}
  >{children}</ProviderLayout>;
}

function EstadoConsulta({ tipo, mensagem, recarregar }: {
  tipo: "carregando" | "vazio" | "erro";
  mensagem?: string;
  recarregar: () => void;
}) {
  return <div className={`${painel} grid min-h-40 place-items-center gap-3 text-center`}>
    <p role={tipo === "erro" ? "alert" : "status"} className="text-sm text-[#637479]">
      {tipo === "carregando" ? "Carregando dados do frete…"
        : tipo === "vazio" ? "Não há dados de designação para este frete."
          : mensagem}
    </p>
    {tipo !== "carregando" && <button className={secundario} type="button" onClick={recarregar}>Tentar novamente</button>}
  </div>;
}

function Cabecalho({ dados, titulo }: { dados: ConsultaDesignacao; titulo: string }) {
  const modalidade = dados.modalidade === "ENCOMENDA" ? "Encomendas" : "Pessoas";
  return <div className="grid gap-2">
    <p className="text-[11px] font-semibold text-[#17695b]">RF009&nbsp; / &nbsp;MUV</p>
    <h1 className="text-2xl font-bold tracking-[-0.02em] sm:text-[30px]">{titulo}</h1>
    <p className="text-sm text-[#637479]">
      FR-{dados.freteId} · {modalidade} · {dados.origem} → {dados.destino} · {dataHora.format(new Date(dados.dataPretendida))}
    </p>
  </div>;
}

function ResumoCondicoes({ dados }: { dados: ConsultaDesignacao }) {
  return <section className={painel}>
    <h2 className="text-lg font-semibold">Condições acordadas</h2>
    <p className="mt-4 text-xl font-bold text-[#17695b]">{moeda.format(dados.valor)}</p>
    <p className="mt-2 whitespace-pre-line text-sm text-[#637479]">{dados.condicoes || "Nenhuma condição adicional registrada."}</p>
  </section>;
}

function descricaoVeiculo(veiculo: VeiculoDisponivel) {
  return `${veiculo.marca} ${veiculo.modelo} · ${veiculo.placa}`;
}

function mensagemFalha(falha: unknown) {
  return falha instanceof Error ? falha.message : "Não foi possível concluir a operação.";
}

export function DesignarRecursosPage() {
  const { id = "" } = useParams();
  return <DesignarRecursos key={id} id={id} />;
}

function DesignarRecursos({ id }: { id: string }) {
  const { estado, recarregar } = useDesignacao(id);
  const { enviando, executar } = useEnvioUnico();
  const [motoristaId, setMotoristaId] = useState("");
  const [veiculoId, setVeiculoId] = useState("");
  const [erro, setErro] = useState("");
  const [confirmacao, setConfirmacao] = useState("");

  if (estado.tipo !== "pronto") return <Layout titulo="Designar motorista e veículo">
    <EstadoConsulta
      tipo={estado.tipo === "erro" ? "erro" : estado.tipo === "vazio" ? "vazio" : "carregando"}
      mensagem={estado.tipo === "erro" ? estado.mensagem : undefined}
      recarregar={recarregar}
    />
  </Layout>;
  const dados = estado.dados;
  const veiculo = dados.veiculos.find(item => String(item.id) === veiculoId);
  const podeSalvar = Boolean(motoristaId && veiculoId && veiculo?.compativel && !enviando);

  async function salvar() {
    if (!podeSalvar) return;
    setErro(""); setConfirmacao("");
    await executar(async () => {
      try {
        await designar(id, Number(motoristaId), Number(veiculoId), dados.revisao);
        setConfirmacao("Designação salva. O frete está pronto para a próxima etapa.");
        recarregar();
      } catch (falha) {
        setErro(mensagemFalha(falha));
        if (falha instanceof DesignacaoApiError && falha.status === 409) recarregar();
      }
    });
  }

  return <Layout titulo="Designar motorista e veículo">
    <div className="grid gap-6">
      <Cabecalho dados={dados} titulo="Designe a equipe para o frete" />
      {!dados.designacaoAtual && <div className="rounded-xl bg-[#fff3de] p-4 text-sm">
        <p className="font-semibold text-[#925705]">Falta designar motorista e veículo</p>
        <p className="mt-1">O início da execução fica bloqueado até haver recursos compatíveis.</p>
      </div>}
      <div className="grid items-start gap-5 xl:grid-cols-[minmax(0,1.35fr)_minmax(300px,.65fr)]">
        <section className={painel}>
          <h2 className="text-lg font-semibold">Motorista e veículo</h2>
          {dados.motoristas.length === 0 || dados.veiculos.length === 0 ? <p className="mt-4 text-sm text-[#637479]">Não há recursos cadastrados disponíveis para concluir a designação.</p> : <>
            <div className="mt-5 grid gap-4 sm:grid-cols-2">
              <label className="text-xs font-semibold text-[#637479]">Motorista vinculado *
                <select className={campo} value={motoristaId} disabled={enviando} onChange={evento => setMotoristaId(evento.target.value)}>
                  <option value="">Selecione um motorista</option>
                  {dados.motoristas.map(item => <option value={item.id} key={item.id}>{item.nome} · Verificado</option>)}
                </select>
              </label>
              <label className="text-xs font-semibold text-[#637479]">Veículo *
                <select className={campo} value={veiculoId} disabled={enviando} onChange={evento => setVeiculoId(evento.target.value)}>
                  <option value="">Selecione um veículo compatível</option>
                  {dados.veiculos.map(item => <option value={item.id} key={item.id} disabled={!item.compativel}>{descricaoVeiculo(item)}{item.compativel ? "" : " · Incompatível"}</option>)}
                </select>
              </label>
            </div>
            <h3 className="mt-6 text-sm font-semibold">Disponíveis para este serviço</h3>
            <div className="mt-3 grid gap-3">
              {dados.veiculos.map(item => <article key={item.id} className="rounded-xl bg-[#f5f7f8] p-4 text-sm">
                <p className="font-semibold">{descricaoVeiculo(item)}</p>
                <p className={`mt-1 ${item.compativel ? "text-[#17695b]" : "text-[#9b3a2e]"}`}>
                  {item.compativel ? `Compatível · ${item.capacidadePassageiros} passageiros · ${item.capacidadeCargaKg} kg`
                    : `Incompatível: ${item.incompatibilidades.join(", ")}`}
                </p>
                {item.compativel && <button type="button" className="mt-3 text-xs font-semibold text-[#17695b] hover:underline" onClick={() => setVeiculoId(String(item.id))}>Selecionar veículo</button>}
              </article>)}
            </div>
            {erro && <p role="alert" className="mt-4 text-sm text-red-700">{erro}</p>}
            {confirmacao && <p role="status" className="mt-4 text-sm font-semibold text-[#17695b]">{confirmacao}</p>}
            <button type="button" className={`${primario} mt-5`} disabled={!podeSalvar} onClick={salvar}>{enviando ? "Salvando…" : "Salvar designação"}</button>
          </>}
        </section>
        <div className="grid gap-5">
          <section className={painel}>
            <h2 className="text-lg font-semibold">Validações do serviço</h2>
            <ul className="mt-4 grid gap-2 text-sm text-[#637479]">
              <li>✓ Prestador responsável pelo frete</li>
              <li>{motoristaId ? "✓ Motorista vinculado e verificado" : "— Motorista ainda não selecionado"}</li>
              <li>{veiculo?.compativel ? "✓ Veículo compatível" : "— Veículo compatível ainda não selecionado"}</li>
              <li>{dados.modalidade === "PESSOA" ? `Capacidade necessária: ${dados.quantidadePessoas ?? 0} passageiro(s)` : `Carga necessária: ${dados.pesoEstimadoKg ?? 0} kg`}</li>
            </ul>
          </section>
          <ResumoCondicoes dados={dados} />
        </div>
      </div>
    </div>
  </Layout>;
}

export function SubstituirRecursosPage() {
  const { id = "" } = useParams();
  return <SubstituirRecursos key={id} id={id} />;
}

function SubstituirRecursos({ id }: { id: string }) {
  const navegar = useNavigate();
  const { estado, recarregar } = useDesignacao(id);
  const { enviando, executar } = useEnvioUnico();
  const [motoristaSelecionado, setMotoristaId] = useState<string>();
  const [veiculoSelecionado, setVeiculoId] = useState<string>();
  const [justificativa, setJustificativa] = useState("");
  const [erro, setErro] = useState("");
  const [confirmacao, setConfirmacao] = useState("");

  const dados = estado.tipo === "pronto" ? estado.dados : null;
  const motoristaId = motoristaSelecionado
    ?? String(dados?.designacaoAtual?.motoristaId ?? "");
  const veiculoId = veiculoSelecionado
    ?? String(dados?.designacaoAtual?.veiculoId ?? "");
  const veiculo = useMemo(() => dados?.veiculos.find(item => String(item.id) === veiculoId), [dados, veiculoId]);

  if (!dados) return <Layout titulo="Substituir recursos">
    <EstadoConsulta
      tipo={estado.tipo === "erro" ? "erro" : estado.tipo === "vazio" ? "vazio" : "carregando"}
      mensagem={estado.tipo === "erro" ? estado.mensagem : undefined}
      recarregar={recarregar}
    />
  </Layout>;
  const atual = dados.designacaoAtual;
  const revisao = dados.revisao;
  const podeConfirmar = Boolean(atual && motoristaId && veiculoId && veiculo?.compativel && justificativa.trim() && !enviando);

  async function confirmar() {
    if (!podeConfirmar) return;
    setErro(""); setConfirmacao("");
    await executar(async () => {
      try {
        const resposta = await substituir(id, {
          motoristaId: Number(motoristaId), veiculoId: Number(veiculoId),
          justificativa: justificativa.trim(), revisao,
        });
        setConfirmacao(resposta.solicitacaoAcordoId
          ? "Solicitação enviada ao cliente. A designação atual foi mantida até a resposta."
          : "Substituição confirmada e cliente notificado.");
        recarregar();
      } catch (falha) {
        setErro(mensagemFalha(falha));
        if (falha instanceof DesignacaoApiError && falha.status === 409) recarregar();
      }
    });
  }

  return <Layout titulo="Substituir recursos">
    <div className="grid gap-6">
      <Cabecalho dados={dados} titulo="Substitua motorista ou veículo" />
      {!atual ? <div className={painel}><p className="text-sm">Este frete ainda não possui uma designação para substituir.</p></div> : <>
        <section className={painel}>
          <h2 className="text-lg font-semibold">Designação atual</h2>
          <dl className="mt-4 grid gap-4 text-sm sm:grid-cols-2">
            <div><dt className="text-xs font-semibold text-[#637479]">Motorista</dt><dd className="mt-1 font-semibold">{atual.motoristaNome}</dd></div>
            <div><dt className="text-xs font-semibold text-[#637479]">Veículo</dt><dd className="mt-1 font-semibold">{atual.veiculoMarca} {atual.veiculoModelo} · {atual.veiculoPlaca}</dd></div>
          </dl>
        </section>
        <section className={painel}>
          <h2 className="text-lg font-semibold">Nova designação</h2>
          <div className="mt-5 grid gap-4 sm:grid-cols-2">
            <label className="text-xs font-semibold text-[#637479]">Novo motorista *
              <select className={campo} value={motoristaId} disabled={enviando} onChange={evento => setMotoristaId(evento.target.value)}>
                <option value="">Selecione um motorista</option>
                {dados.motoristas.map(item => <option value={item.id} key={item.id}>{item.nome} · Vinculado e verificado</option>)}
              </select>
            </label>
            <label className="text-xs font-semibold text-[#637479]">Novo veículo *
              <select className={campo} value={veiculoId} disabled={enviando} onChange={evento => setVeiculoId(evento.target.value)}>
                <option value="">Selecione um veículo</option>
                {dados.veiculos.map(item => <option value={item.id} key={item.id} disabled={!item.compativel}>{descricaoVeiculo(item)}{item.compativel ? "" : " · Incompatível"}</option>)}
              </select>
            </label>
          </div>
          <div className={`mt-4 rounded-xl p-4 text-sm ${veiculo?.compativel ? "bg-[#e5f2ee] text-[#17695b]" : "bg-[#fff3de] text-[#925705]"}`}>
            {veiculo?.compativel ? "Compatibilidade confirmada" : "Selecione recursos compatíveis para continuar."}
          </div>
          <label className="mt-5 block text-xs font-semibold text-[#637479]">Justificativa da substituição *
            <textarea className="mt-2 min-h-28 w-full resize-y rounded-lg border border-[#d8e2e5] bg-[#f8fafb] p-3 text-sm outline-none focus:border-[#17695b]" value={justificativa} disabled={enviando} onChange={evento => setJustificativa(evento.target.value)} />
          </label>
          <div className="mt-4 rounded-xl bg-[#f5f7f8] p-4 text-sm">
            <p className="font-semibold text-[#17695b]">O cliente será notificado</p>
            <p className="mt-1 text-[#637479]">A alteração registra equipe anterior, nova equipe, responsável, data, horário e justificativa.</p>
          </div>
          {erro && <p role="alert" className="mt-4 text-sm text-red-700">{erro}</p>}
          {confirmacao && <p role="status" className="mt-4 text-sm font-semibold text-[#17695b]">{confirmacao}</p>}
          <div className="mt-5 flex flex-wrap gap-3">
            <button type="button" className={primario} disabled={!podeConfirmar} onClick={confirmar}>{enviando ? "Confirmando…" : "Confirmar substituição"}</button>
            <button type="button" className={secundario} disabled={enviando} onClick={() => navegar(-1)}>Manter designação atual</button>
          </div>
        </section>
      </>}
    </div>
  </Layout>;
}
