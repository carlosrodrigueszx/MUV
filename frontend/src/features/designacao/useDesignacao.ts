import { useCallback, useEffect, useReducer, useRef, useState } from "react";
import { DesignacaoApiError, consultarDesignacao } from "./api";
import {
  concluirConsulta,
  falharConsulta,
  iniciarConsulta,
  type EstadoConsultaDesignacao,
} from "./state";

type Acao =
  | { tipo: "iniciar"; freteId: string }
  | { tipo: "sucesso"; freteId: string; dados: Parameters<typeof concluirConsulta>[2] }
  | { tipo: "falha"; freteId: string; mensagem: string; status?: number };

function reduzir(estado: EstadoConsultaDesignacao, acao: Acao): EstadoConsultaDesignacao {
  if (acao.tipo === "iniciar") return iniciarConsulta(acao.freteId);
  if (acao.tipo === "sucesso") return concluirConsulta(estado, acao.freteId, acao.dados);
  return falharConsulta(estado, acao.freteId, acao.mensagem, acao.status);
}

export function useDesignacao(freteId: string) {
  const [estado, despachar] = useReducer(reduzir, { tipo: "inicial" });
  const [atualizacao, setAtualizacao] = useState(0);

  const recarregar = useCallback(() => setAtualizacao(valor => valor + 1), []);

  useEffect(() => {
    const controle = new AbortController();
    despachar({ tipo: "iniciar", freteId });
    consultarDesignacao(freteId, controle.signal)
      .then(dados => despachar({ tipo: "sucesso", freteId, dados }))
      .catch(falha => {
        if (controle.signal.aborted) return;
        despachar({
          tipo: "falha",
          freteId,
          mensagem: falha instanceof Error ? falha.message : "Não foi possível carregar o frete.",
          status: falha instanceof DesignacaoApiError ? falha.status : undefined,
        });
      });
    return () => controle.abort();
  }, [freteId, atualizacao]);

  return { estado, recarregar };
}

export function useEnvioUnico() {
  const trava = useRef(false);
  const [enviando, setEnviando] = useState(false);

  const executar = useCallback(async (acao: () => Promise<void>) => {
    if (trava.current) return;
    trava.current = true;
    setEnviando(true);
    try {
      await acao();
    } finally {
      trava.current = false;
      setEnviando(false);
    }
  }, []);

  return { enviando, executar };
}
