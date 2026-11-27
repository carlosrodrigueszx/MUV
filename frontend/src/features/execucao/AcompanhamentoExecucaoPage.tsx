import { useEffect, useState } from "react";
import { LuArrowRight } from "react-icons/lu";
import { useParams } from "react-router-dom";
import { ProviderLayout } from "../../components/layout/ProviderLayout";
import { providerNavItems } from "../../components/layout/providerNav";
import { focarAoMontar } from "./foco";
import type { DadosFormulario } from "./FormularioExecucao";
import { ConclusaoFormulario } from "./ConclusaoFormulario";
import { TransicaoFormulario } from "./TransicaoFormulario";
import { type Acompanhamento, type AcaoExecucao, type AcaoTransicao, type TransicaoConfirmada, consultarExecucao } from "./api";

const estados: Record<string, string> = {
  AGUARDANDO_DESIGNACAO: "Aguardando designação",
  PRONTO_INICIO: "Pronto para início",
  EM_EXECUCAO: "Em execução",
  INTERROMPIDO: "Interrompido",
  ENCERRADO_SEM_CONCLUSAO: "Encerrado sem conclusão",
  CONCLUIDO: "Concluído",
  CANCELADO: "Cancelado",
};

const rotulos: Record<AcaoExecucao | "CONCLUSAO", string> = {
  CONCLUSAO: "Frete concluído",
  INICIO: "Execução iniciada",
  OCORRENCIA: "Ocorrência registrada",
  INTERRUPCAO: "Interrupção registrada",
  RETOMADA: "Execução retomada",
  ENCERRAMENTO_SEM_CONCLUSAO: "Encerrado sem conclusão",
};

type Consulta =
  | { tipo: "carregando" }
  | { tipo: "erro"; mensagem: string }
  | { tipo: "pronto"; dados: Acompanhamento };

export default function AcompanhamentoExecucaoPage() {
  const { freteId } = useParams();
  if (!freteId || !/^[1-9]\d*$/.test(freteId)) {
    return <main className="p-6"><p role="alert">Frete inválido.</p></main>;
  }
  return <AcompanhamentoDoFrete key={freteId} freteId={freteId} />;
}

function AcompanhamentoDoFrete({ freteId }: { freteId: string }) {
  const [consulta, setConsulta] = useState<Consulta>({ tipo: "carregando" });
  const [tentativa, setTentativa] = useState(0);
  const [conclusao, setConclusao] = useState(false);
  const [concluidoAgora, setConcluidoAgora] = useState(false);
  const [acao, setAcao] = useState<AcaoTransicao | null>(null);
  const [rascunho, setRascunho] = useState<DadosFormulario>();
  const [recibo, setRecibo] = useState<TransicaoConfirmada | null>(null);

  function atualizar() {
    setAcao(null);
    setConclusao(false);
    setConsulta({ tipo: "carregando" });
    setTentativa((anterior) => anterior + 1);
  }

  useEffect(() => {
    const controller = new AbortController();
    consultarExecucao(freteId, controller.signal)
      .then((dados) => {
        if (!controller.signal.aborted) setConsulta({ tipo: "pronto", dados });
      })
      .catch((erro: unknown) => {
        if (!controller.signal.aborted) {
          setConsulta({
            tipo: "erro",
            mensagem: erro instanceof Error ? erro.message : "Não foi possível consultar o frete.",
          });
        }
      });
    return () => controller.abort();
  }, [freteId, tentativa]);

  if (consulta.tipo === "carregando") return <main className="p-6"><p role="status">Carregando frete…</p></main>;
  if (consulta.tipo === "erro") {
    return (
      <main className="space-y-3 p-6">
        {recibo && <p role="status">{mensagemRecibo(recibo)}</p>}
        <p tabIndex={-1} ref={focarAoMontar} role="alert">{consulta.mensagem}</p>
        <button type="button" className="rounded border px-3 py-2" onClick={atualizar}>
          Tentar novamente
        </button>
      </main>
    );
  }
  const { dados } = consulta;
  const prestador = dados.prestadorResponsavel;
  const titulo = conclusao ? "Registrar conclusão" : acao ? titulosFormulario[acao] : "Acompanhamento da execução";
  const navItems = providerNavItems.flatMap((item) => {
    if (item.key === "freights") return [{ ...item, to: `/fretes/${dados.freteId}/execucao` }];
    return prestador ? [item] : [];
  });
  return (
    <ProviderLayout
      pageTitle={titulo}
      activeNavKey="freights"
      isOnline={null}
      navItems={navItems}
      organizationName={dados.usuarioAutenticado}
      portalLabel={prestador ? undefined : "PORTAL DO MOTORISTA"}
      areaLabel={prestador ? undefined : "Área do motorista"}
      profileLabel={prestador ? undefined : "Motorista designado"}
    >
      {conclusao ? (
        <ConclusaoFormulario dados={dados} onVoltar={atualizar} onConcluido={() => {
          setConcluidoAgora(true);
          atualizar();
        }} />
      ) : acao ? (
        <TransicaoFormulario key={acao} acao={acao} contexto={dados} rascunho={rascunho?.acao === acao ? rascunho : undefined}
          onAtualizar={(dados) => { setRascunho(dados); atualizar(); }}
          onVoltar={() => { setRascunho(undefined); atualizar(); }}
          onConfirmada={(resultado) => {
            setRascunho(undefined);
            setRecibo(resultado);
            atualizar();
          }} />
      ) : (
        <ResumoExecucao dados={dados} recibo={recibo} concluidoAgora={concluidoAgora}
          onConcluir={() => setConclusao(true)} onAcao={(proxima) => {
          if (rascunho?.acao !== proxima) setRascunho(undefined);
          setRecibo(null);
          setAcao(proxima);
        }} />
      )}
    </ProviderLayout>
  );
}

const acoesDisponiveis: { acao: AcaoTransicao | "CONCLUSAO"; rotulo: string }[] = [
  { acao: "CONCLUSAO", rotulo: "Registrar conclusão" },
  { acao: "INICIO", rotulo: "Iniciar execução" },
  { acao: "OCORRENCIA", rotulo: "Registrar ocorrência" },
  { acao: "INTERRUPCAO", rotulo: "Registrar interrupção" },
  { acao: "RETOMADA", rotulo: "Retomar execução" },
  { acao: "ENCERRAMENTO_SEM_CONCLUSAO", rotulo: "Encerrar sem conclusão" },
];

function ResumoExecucao({ dados, recibo, onAcao, onConcluir, concluidoAgora }: {
  dados: Acompanhamento;
  recibo: TransicaoConfirmada | null;
  onAcao: (acao: AcaoTransicao) => void;
  onConcluir: () => void;
  concluidoAgora: boolean;
}) {
  return (
    <>
      <h1 className="text-2xl font-bold">Acompanhamento da execução</h1>
      <p className="mt-2 text-[#637479]">FR-{dados.freteId} · {estados[dados.estado] ?? dados.estado}</p>
      {recibo && <p tabIndex={-1} ref={focarAoMontar} role="status"
        className="mt-4 rounded-lg border border-[#bfd9d1] bg-[#e5f2ee] p-4 font-semibold text-[#104e43]">
        {mensagemRecibo(recibo)} Confira o registro no histórico abaixo.
      </p>}
      {concluidoAgora && <div tabIndex={-1} ref={focarAoMontar}
        className="mt-4 space-y-2 rounded-lg border border-[#bfd9d1] bg-[#e5f2ee] p-4 text-[#104e43]">
        <p role="status"><strong>Conclusão registrada.</strong> O cliente foi notificado.</p>
        <a href="#registros-execucao" className="inline-flex min-h-11 items-center font-semibold underline">
          Ver registros de execução
        </a>
      </div>}
      {dados.acoesElegiveis.length > 0 && (
        <section aria-labelledby="acoes-frete" className="mt-5 rounded-xl border border-[#dfe6e7] bg-white p-4">
          <h2 id="acoes-frete" className="font-semibold">Ações para este frete</h2>
          <div className="mt-3 flex flex-wrap gap-3">
            {acoesDisponiveis.filter(({ acao }) => dados.acoesElegiveis.includes(acao)).map(({ acao, rotulo }) => (
              <button key={acao} type="button" onClick={acao === "CONCLUSAO" ? onConcluir : () => onAcao(acao)}
                className={`inline-flex min-h-12 w-full cursor-pointer items-center justify-between gap-3 rounded-lg border px-4 py-2 text-left text-base font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 sm:w-auto ${acao === "ENCERRAMENTO_SEM_CONCLUSAO"
                  ? "border-[#a63c3c] text-[#8b3030] hover:bg-red-50 focus-visible:outline-[#a63c3c]"
                  : "border-[#17695b] bg-[#17695b] text-white hover:bg-[#12584d] focus-visible:outline-[#17695b]"}`}>
                {rotulo}
                <LuArrowRight aria-hidden="true" className="size-4 shrink-0" />
              </button>
            ))}
          </div>
        </section>
      )}
      <div className="mt-5 grid gap-4 lg:grid-cols-2">
        <section className="rounded-xl border border-[#dfe6e7] bg-white p-5">
          <h2 className="font-semibold">Recursos atuais</h2>
          {dados.recursos ? (
            <dl className="mt-3 space-y-2 text-sm">
              <div><dt className="font-medium">Motorista</dt><dd>{dados.recursos.motorista}</dd></div>
              <div><dt className="font-medium">Veículo</dt><dd>{dados.recursos.veiculo} · {dados.recursos.placa}</dd></div>
            </dl>
          ) : <p className="mt-3 text-sm">Motorista e veículo ainda não designados.</p>}
        </section>
        <section className="rounded-xl border border-[#dfe6e7] bg-white p-5">
          <h2 id="registros-execucao" tabIndex={-1} className="scroll-mt-6 font-semibold">Registros de execução</h2>
          {dados.eventos.length === 0 ? <p className="mt-3 text-sm">Nenhum registro de execução.</p> : (
            <ol className="mt-3 divide-y divide-[#dfe6e7] text-sm">
              {dados.eventos.map((evento) => (
                <li className="break-words py-3" key={evento.id}>
                  <p className="font-medium">{rotulos[evento.tipo]}</p>
                  <p>Por {evento.autor}</p>
                  {evento.horarioInformado && <p>Horário informado: <time dateTime={evento.horarioInformado}>{
                    new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short", timeZone: "UTC" })
                      .format(new Date(`${evento.horarioInformado}Z`))
                  }</time></p>}
                  {evento.localOcorrencia && <p>Local da ocorrência: {evento.localOcorrencia}</p>}
                  {evento.situacaoEncomenda && <p>Situação da encomenda: {evento.situacaoEncomenda}</p>}
                  {evento.descricao && <p>{evento.descricao}</p>}
                  {evento.justificativa && <p>{evento.justificativa}</p>}
                </li>
              ))}
            </ol>
          )}
        </section>
      </div>
    </>
  );
}

function mensagemRecibo(recibo: TransicaoConfirmada) {
  if (recibo.estadoAnterior === "PRONTO_INICIO") return "Execução iniciada.";
  switch (recibo.estadoResultante) {
    case "INTERROMPIDO": return "Interrupção registrada.";
    case "EM_EXECUCAO": return recibo.estadoAnterior === "EM_EXECUCAO" ? "Ocorrência registrada." : "Retomada registrada.";
    case "ENCERRADO_SEM_CONCLUSAO": return "Encerramento sem conclusão registrado.";
  }
}

const titulosFormulario: Record<AcaoTransicao, string> = {
  INICIO: "Iniciar execução",
  OCORRENCIA: "Registrar ocorrência e interrupção",
  INTERRUPCAO: "Registrar ocorrência e interrupção",
  RETOMADA: "Retomar execução",
  ENCERRAMENTO_SEM_CONCLUSAO: "Encerrar sem conclusão",
};
