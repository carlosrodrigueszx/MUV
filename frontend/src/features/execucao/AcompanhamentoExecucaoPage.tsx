import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { ProviderLayout } from "../../components/layout/ProviderLayout";
import { providerNavItems } from "../../components/layout/providerNav";
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

const rotulos: Record<AcaoExecucao, string> = {
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
  const [acao, setAcao] = useState<AcaoTransicao | null>(null);
  const [recibo, setRecibo] = useState<TransicaoConfirmada | null>(null);

  function atualizar() {
    setAcao(null);
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
        <p role="alert">{consulta.mensagem}</p>
        <button type="button" className="rounded border px-3 py-2" onClick={atualizar}>
          Tentar novamente
        </button>
      </main>
    );
  }
  const { dados } = consulta;
  const prestador = dados.prestadorResponsavel;
  const titulo = acao ? titulosFormulario[acao] : "Acompanhamento da execução";
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
      {acao ? (
        <TransicaoFormulario key={acao} acao={acao} contexto={dados} onAtualizar={atualizar} onVoltar={atualizar}
          onConfirmada={(resultado) => {
            setRecibo(resultado);
            atualizar();
          }} />
      ) : (
        <ResumoExecucao dados={dados} recibo={recibo} onAcao={setAcao} />
      )}
    </ProviderLayout>
  );
}

function ResumoExecucao({ dados, recibo, onAcao }: {
  dados: Acompanhamento;
  recibo: TransicaoConfirmada | null;
  onAcao: (acao: AcaoTransicao) => void;
}) {
  return (
    <>
      <h1 className="text-2xl font-bold">Acompanhamento da execução</h1>
      <p className="mt-2 text-[#637479]">FR-{dados.freteId} · {estados[dados.estado] ?? dados.estado}</p>
      {recibo && <p role="status" className="mt-2 text-sm text-[#17695b]">{mensagemRecibo(recibo)}</p>}
      {dados.acoesElegiveis.includes("INTERRUPCAO") && (
        <button type="button" onClick={() => onAcao("INTERRUPCAO")}
          className="mt-4 rounded-lg bg-[#17695b] px-3 py-2 text-sm font-semibold text-white">
          Registrar interrupção
        </button>
      )}
      {dados.acoesElegiveis.includes("RETOMADA") && (
        <button type="button" onClick={() => onAcao("RETOMADA")}
          className="mt-4 rounded-lg bg-[#17695b] px-3 py-2 text-sm font-semibold text-white">
          Retomar execução
        </button>
      )}
      {dados.acoesElegiveis.includes("ENCERRAMENTO_SEM_CONCLUSAO") && (
        <button type="button" onClick={() => onAcao("ENCERRAMENTO_SEM_CONCLUSAO")}
          className="mt-4 ml-3 rounded-lg border border-[#a63c3c] px-3 py-2 text-sm font-semibold text-[#a63c3c]">
          Encerrar sem conclusão
        </button>
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
          <h2 className="font-semibold">Registros de execução</h2>
          {dados.eventos.length === 0 ? <p className="mt-3 text-sm">Nenhum registro de execução.</p> : (
            <ol className="mt-3 divide-y divide-[#dfe6e7] text-sm">
              {dados.eventos.map((evento) => (
                <li className="break-words py-3" key={evento.id}>
                  <p className="font-medium">{rotulos[evento.tipo]}</p>
                  <p>Por {evento.autor}</p>
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
  switch (recibo.estadoResultante) {
    case "INTERROMPIDO": return "Interrupção registrada.";
    case "EM_EXECUCAO": return "Retomada registrada.";
    case "ENCERRADO_SEM_CONCLUSAO": return "Encerramento sem conclusão registrado.";
  }
}

const titulosFormulario: Record<AcaoTransicao, string> = {
  INTERRUPCAO: "Registrar interrupção",
  RETOMADA: "Retomar execução",
  ENCERRAMENTO_SEM_CONCLUSAO: "Encerrar sem conclusão",
};
