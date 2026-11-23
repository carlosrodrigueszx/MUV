import { useRef, useState } from "react";
import { focarAoMontar } from "./foco";
import { FormularioExecucao, type DadosFormulario } from "./FormularioExecucao";
import {
  type Acompanhamento, type AcaoTransicao, type ComandoInicio, type ComandoTransicao, type TransicaoConfirmada,
  ErroTransicao, registrarTransicao,
} from "./api";

type Props = {
  contexto: Acompanhamento;
  acao: AcaoTransicao;
  onConfirmada: (resultado: TransicaoConfirmada) => void;
  onVoltar: () => void;
  rascunho?: DadosFormulario;
  onAtualizar: (dados?: DadosFormulario) => void;
};

export function TransicaoFormulario({ contexto, acao, onConfirmada, onVoltar, onAtualizar, rascunho }: Props) {
  const tentativa = useRef<ComandoInicio | ComandoTransicao | null>(null);
  const [somenteReenvio, setSomenteReenvio] = useState(false);
  const [exigeAtualizacao, setExigeAtualizacao] = useState(false);

  const [enviandoInicio, setEnviandoInicio] = useState(false);
  const [erroInicio, setErroInicio] = useState("");
  const inicioEmCurso = useRef(false);

  async function confirmar(dados?: DadosFormulario) {
    const selecionada = dados?.acao ?? acao;
    const base = () => ({
      chaveOperacao: crypto.randomUUID(),
      estadoEsperado: contexto.estado,
      revisaoDesignacao: contexto.revisaoDesignacao,
    });
    if (acao === "INICIO") {
      tentativa.current ??= base();
    } else {
      if (!dados) return;
      const usaCiclo = selecionada === "INTERRUPCAO" || selecionada === "OCORRENCIA";
      const referencia = usaCiclo ? contexto.cicloExecucaoId : contexto.interrupcaoId;
      if (referencia == null || exigeAtualizacao) return;
      tentativa.current ??= {
        ...base(),
        ...(usaCiclo
          ? { cicloExecucaoId: referencia }
          : { interrupcaoId: referencia, confirmou: dados.confirmou, situacaoEncomenda: dados.situacaoEncomenda }),
        ...(selecionada === "OCORRENCIA" ? { descricao: dados.texto } : { justificativa: dados.texto }),
        data: dados.data,
        horario: dados.horario,
      };
    }
    let resultado: TransicaoConfirmada;
    try {
      resultado = await registrarTransicao(contexto.freteId, selecionada, tentativa.current);
    } catch (erro) {
      if (erro instanceof ErroTransicao && [400, 401, 403, 404, 409].includes(erro.status)) {
        tentativa.current = null;
        setSomenteReenvio(false);
        setExigeAtualizacao(erro.status !== 400);
        throw erro;
      }
      setSomenteReenvio(true);
      throw new Error("A confirmação ficou sem resposta. Tente novamente para recuperar o mesmo registro.", {
        cause: erro,
      });
    }
    onConfirmada(resultado);
  }

  if (acao === "INICIO") {
    async function iniciar() {
      if (inicioEmCurso.current || exigeAtualizacao) return;
      inicioEmCurso.current = true;
      setEnviandoInicio(true);
      try {
        await confirmar();
      } catch (erro) {
        setErroInicio(erro instanceof Error ? erro.message : "Não foi possível confirmar.");
      } finally {
        inicioEmCurso.current = false;
        setEnviandoInicio(false);
      }
    }
    return (
      <section className="space-y-3">
        <h1 ref={focarAoMontar} tabIndex={-1} className="text-2xl font-bold">Iniciar execução</h1>
        <p>FR-{contexto.freteId} · Após confirmar, o frete ficará em execução.</p>
        {erroInicio && <p ref={focarAoMontar} tabIndex={-1} role="alert">{erroInicio}</p>}
        {exigeAtualizacao ? <button onClick={() => onAtualizar()}>Atualizar acompanhamento</button> : (
          <button className="rounded-lg bg-[#17695b] px-3 py-2 text-white" disabled={enviandoInicio} onClick={iniciar}>
            {enviandoInicio ? "Confirmando…" : somenteReenvio ? "Tentar novamente" : "Confirmar início"}
          </button>
        )}
        <button className="ml-3" disabled={enviandoInicio} onClick={onVoltar}>Voltar ao acompanhamento</button>
      </section>
    );
  }
  return (
    <FormularioExecucao
      contexto={contexto}
      acaoInicial={acao}
      rascunho={rascunho}
      somenteReenvio={somenteReenvio}
      onAtualizar={exigeAtualizacao ? onAtualizar : undefined}
      onConfirmar={confirmar}
      onVoltar={onVoltar}
    />
  );
}
