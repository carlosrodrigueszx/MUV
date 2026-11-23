import { useRef, useState } from "react";
import { FormularioExecucao, type DadosFormulario } from "./FormularioExecucao";
import {
  type Acompanhamento, type AcaoTransicao, type ComandoTransicao, type TransicaoConfirmada,
  ErroTransicao, registrarTransicao,
} from "./api";

type Props = {
  contexto: Acompanhamento;
  acao: AcaoTransicao;
  onConfirmada: (resultado: TransicaoConfirmada) => void;
  onVoltar: () => void;
  onAtualizar: () => void;
};

export function TransicaoFormulario({ contexto, acao, onConfirmada, onVoltar, onAtualizar }: Props) {
  const tentativa = useRef<ComandoTransicao | null>(null);
  const [somenteReenvio, setSomenteReenvio] = useState(false);
  const [exigeAtualizacao, setExigeAtualizacao] = useState(false);

  async function confirmar(dados: DadosFormulario) {
    const referencia = acao === "INTERRUPCAO" ? contexto.cicloExecucaoId : contexto.interrupcaoId;
    if (referencia == null || exigeAtualizacao) return;
    tentativa.current ??= {
      chaveOperacao: crypto.randomUUID(),
      estadoEsperado: contexto.estado,
      revisaoDesignacao: contexto.revisaoDesignacao,
      ...(acao === "INTERRUPCAO"
        ? { cicloExecucaoId: referencia }
        : { interrupcaoId: referencia, confirmou: dados.confirmou, situacaoEncomenda: dados.situacaoEncomenda }),
      justificativa: dados.texto,
      data: dados.data,
      horario: dados.horario,
    };
    let resultado: TransicaoConfirmada;
    try {
      resultado = await registrarTransicao(contexto.freteId, acao, tentativa.current);
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

  return (
    <FormularioExecucao
      contexto={contexto}
      acaoInicial={acao}
      acaoFixa
      somenteReenvio={somenteReenvio}
      onAtualizar={exigeAtualizacao ? onAtualizar : undefined}
      onConfirmar={confirmar}
      onVoltar={onVoltar}
    />
  );
}
