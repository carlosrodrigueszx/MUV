import { type FormEvent, useRef, useState } from "react";
import { focarAoMontar } from "./foco";
import type { AcaoExecucao, Acompanhamento } from "./api";

type AcaoFormulario = Exclude<AcaoExecucao, "INICIO">;

const rotulos = {
  OCORRENCIA: {
    titulo: "Registre uma ocorrência", texto: "Descrição da ocorrência",
    efeito: "O frete permanece em execução.", confirmar: "Registrar ocorrência",
    voltar: "Voltar sem registrar", aceite: null,
  },
  INTERRUPCAO: {
    titulo: "Registre uma ocorrência", texto: "Justificativa obrigatória",
    efeito: "A situação mudará para Interrompido.", confirmar: "Registrar e interromper execução",
    voltar: "Voltar sem registrar", aceite: null,
  },
  RETOMADA: {
    titulo: "Retome a execução com segurança", texto: "Justificativa da retomada",
    efeito: "Após confirmar: Em execução", confirmar: "Confirmar retomada",
    voltar: "Voltar ao acompanhamento", aceite: "Confirmo que é possível prosseguir com o serviço.",
  },
  ENCERRAMENTO_SEM_CONCLUSAO: {
    titulo: "Encerre sem marcar como concluído", texto: "Justificativa obrigatória",
    efeito: "Esta ação não é uma conclusão",
    confirmar: "Encerrar sem conclusão",
    voltar: "Voltar e manter interrompido", aceite: "Confirmo que o frete não foi concluído.",
  },
} satisfies Record<AcaoFormulario, {
  titulo: string; texto: string; efeito: string; confirmar: string; voltar: string; aceite: string | null;
}>;

export type DadosFormulario = {
  acao: AcaoFormulario;
  texto: string;
  data: string;
  horario: string;
  confirmou: boolean;
  situacaoEncomenda?: string;
  localOcorrencia?: string;
};

type Props = {
  contexto: Acompanhamento;
  acaoInicial: AcaoFormulario;
  onConfirmar: (dados: DadosFormulario) => Promise<void>;
  onVoltar: () => void;
  somenteReenvio?: boolean;
  rascunho?: DadosFormulario;
  onAtualizar?: (dados: DadosFormulario) => void;
};

const campo = "w-full rounded-lg border border-[#dfe6e7] bg-[#f5f7f8] p-2 text-sm";

export function FormularioExecucao({
  contexto, acaoInicial, onConfirmar, onVoltar, somenteReenvio = false, onAtualizar, rascunho,
}: Props) {
  const [acao, setAcao] = useState(acaoInicial);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const formulario = useRef<HTMLFormElement>(null);
  const envioEmCurso = useRef(false);
  const textos = rotulos[acao];
  const interrompido = acao === "RETOMADA" || acao === "ENCERRAMENTO_SEM_CONCLUSAO";
  const encerramento = acao === "ENCERRAMENTO_SEM_CONCLUSAO";
  const interrupcao = contexto.eventos.find((evento) => evento.id === contexto.interrupcaoId);

  function dadosDoFormulario(form: HTMLFormElement): DadosFormulario {
    const campos = new FormData(form);
    const situacao = String(campos.get("situacaoEncomenda") ?? "");
    return {
      acao, texto: String(campos.get("texto") ?? ""), data: String(campos.get("data") ?? ""),
      horario: String(campos.get("horario") ?? ""), confirmou: campos.has("confirmou"),
      situacaoEncomenda: situacao.trim() ? situacao : undefined,
      localOcorrencia: String(campos.get("localOcorrencia") ?? "").trim() || undefined,
    };
  }

  async function confirmar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (envioEmCurso.current || onAtualizar) return;
    const dados = dadosDoFormulario(evento.currentTarget);
    if (/^\p{White_Space}*$/u.test(dados.texto)) {
      setErro(`Preencha o campo ${textos.texto.toLocaleLowerCase("pt-BR")}.`);
      return;
    }
    envioEmCurso.current = true;
    setEnviando(true);
    setErro(null);
    try {
      await onConfirmar(dados);
    } catch (falha: unknown) {
      setErro(falha instanceof Error ? falha.message : "Não foi possível confirmar.");
    } finally {
      envioEmCurso.current = false;
      setEnviando(false);
    }
  }

  return (
    <>
      <p className="mb-5 text-xs font-semibold text-[#17695b]">RF010 / MUV</p>
      <h1 tabIndex={-1} ref={focarAoMontar} className="text-2xl font-bold">{textos.titulo}</h1>
      <p className="mt-2 text-sm text-[#637479]">
        FR-{contexto.freteId} · {interrompido ? "Interrompido" : "Em execução"}{!interrompido && ` · Usuário autorizado: ${contexto.usuarioAutenticado}`}
        {encerramento && " · Use esta opção se não for possível retomar o serviço."}
        {acao === "RETOMADA" && interrupcao?.horarioRegistro && ` desde ${new Intl.DateTimeFormat("pt-BR", {
          dateStyle: "medium", timeStyle: "short", timeZone: "UTC",
        }).format(new Date(interrupcao.horarioRegistro))} UTC`}
      </p>
      <div className="mt-5 grid gap-4 lg:grid-cols-2">
        <form ref={formulario} className="min-w-0 space-y-4 rounded-xl border border-[#dfe6e7] bg-white p-5" onSubmit={confirmar}>
          <h2 className="text-lg font-semibold">{encerramento ? "Motivo do encerramento" : interrompido ? "Registro de retomada" : "O que aconteceu?"}</h2>
          {!interrompido && (
            <>
            <label className="block text-sm font-medium">
              Tipo de registro *
              <select name="tipo" required className={campo} value={acao} disabled={enviando || somenteReenvio || Boolean(onAtualizar)} onChange={(evento) => {
                setAcao(evento.target.value === "INTERRUPCAO" ? "INTERRUPCAO" : "OCORRENCIA");
                setErro(null);
              }}>
                <option disabled={!contexto.acoesElegiveis.includes("OCORRENCIA")} value="OCORRENCIA">Ocorrência sem interrupção</option>
                <option disabled={!contexto.acoesElegiveis.includes("INTERRUPCAO")} value="INTERRUPCAO">Interrupção da execução</option>
              </select>
            </label>
            <p className="text-xs text-[#637479]">Retomada e encerramento sem conclusão ficam disponíveis quando o frete está interrompido.</p>
            </>
          )}
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="block text-sm font-medium">Data * <input className={campo} name="data" defaultValue={rascunho?.data} type="date" required disabled={enviando} readOnly={somenteReenvio} /></label>
            <label className="block text-sm font-medium">Horário * <input className={campo} name="horario" defaultValue={rascunho?.horario} type="time" required disabled={enviando} readOnly={somenteReenvio} /></label>
          </div>
          <label className="block text-sm font-medium">
            {textos.texto} *
            <textarea className={campo} name="texto" defaultValue={rascunho?.texto} required rows={3} disabled={enviando} readOnly={somenteReenvio} />
          </label>
          {!interrompido && <label className="block text-sm font-medium">
            Local da ocorrência
            <input name="localOcorrencia" className={campo} defaultValue={rascunho?.localOcorrencia}
              disabled={enviando} readOnly={somenteReenvio} />
          </label>}
          {acao === "RETOMADA" && contexto.recursos && (
            <div className="rounded-lg bg-[#e5f2ee] p-3 text-sm">
              <p className="font-medium">Recursos confirmados</p>
              <p>{contexto.recursos.motorista} · {contexto.recursos.veiculo} · {contexto.recursos.placa} · Compatíveis com {contexto.modalidade === "ENCOMENDA" ? "encomendas" : "transporte de pessoas"}</p>
            </div>
          )}
          {acao === "ENCERRAMENTO_SEM_CONCLUSAO" && contexto.modalidade === "ENCOMENDA" && (
            <label className="block text-sm font-medium">
              Situação da encomenda (opcional)
              <textarea className={campo} name="situacaoEncomenda" defaultValue={rascunho?.situacaoEncomenda} rows={2}
                disabled={enviando} readOnly={somenteReenvio} />
            </label>
          )}
          {textos.aceite && (
            <label className="flex items-start gap-2 text-sm">
              <input name="confirmou" type="checkbox" required disabled={enviando || somenteReenvio} />
              {textos.aceite}
            </label>
          )}
          {erro && <p ref={focarAoMontar} tabIndex={-1} role="alert" className="text-sm text-red-700">{erro}</p>}
          <button type="submit" disabled={enviando || Boolean(onAtualizar)} className={
            `rounded-lg px-3 py-2 text-sm font-semibold text-white disabled:opacity-60 ${acao === "ENCERRAMENTO_SEM_CONCLUSAO" ? "bg-[#a63c3c]" : "bg-[#17695b]"}`
          }>
            {enviando ? "Confirmando…" : somenteReenvio ? "Tentar novamente" : textos.confirmar}
          </button>
        </form>
        <aside className="min-w-0 space-y-3 self-start rounded-xl border border-[#dfe6e7] bg-white p-5 text-sm">
          <h2 className="font-semibold">{
            acao === "RETOMADA" ? "Interrupção em aberto"
              : acao === "ENCERRAMENTO_SEM_CONCLUSAO" ? "Revise antes de confirmar" : "Efeito deste registro"
          }</h2>
          {acao === "RETOMADA" && interrupcao && (
            <div className="break-words rounded-lg bg-[#f5f7f8] p-3">
              {interrupcao.horarioRegistro && (
                <p>Horário do registro: <time dateTime={interrupcao.horarioRegistro}>{
                  new Intl.DateTimeFormat("pt-BR", {
                    dateStyle: "short", timeStyle: "short", timeZone: "UTC",
                  }).format(new Date(interrupcao.horarioRegistro))
                } UTC</time></p>
              )}
              <p className="font-medium">{interrupcao.justificativa}</p>
              <p>Por {interrupcao.autor}</p>
            </div>
          )}
          <p className={`rounded-lg p-3 ${acao === "INTERRUPCAO" || acao === "ENCERRAMENTO_SEM_CONCLUSAO" ? "bg-[#fff2dd]" : "bg-[#e5f2ee]"}`}>
            <strong className="block">{textos.efeito}</strong>
            {encerramento && "O histórico registrará o encerramento sem conclusão, com o motivo e o responsável."}
            {acao === "RETOMADA" && "A retomada e sua justificativa serão adicionadas ao histórico."}
          </p>
          {!interrompido && <p>O registro preserva autor, data, horário e {acao === "OCORRENCIA" ? "descrição" : "justificativa"} no histórico. O serviço não será marcado como concluído.</p>}
          {encerramento && <p>Nenhuma entrega será registrada como realizada.</p>}
          {acao === "ENCERRAMENTO_SEM_CONCLUSAO" && (
            <div className="rounded-lg bg-[#f5f7f8] p-3">
              <p className="font-medium">Responsável pelo registro</p>
              <p>{contexto.usuarioAutenticado}</p>
            </div>
          )}
          {!interrompido && contexto.recursos && (
            <div className="rounded-lg bg-[#f5f7f8] p-3">
              <p className="font-medium">Recursos atuais</p>
              <p>{contexto.recursos.motorista}</p>
              <p>{contexto.recursos.veiculo} · {contexto.recursos.placa}</p>
            </div>
          )}
          {onAtualizar && (
            <button type="button" onClick={() => formulario.current && onAtualizar(dadosDoFormulario(formulario.current))} className="block font-semibold text-[#17695b]">
              Atualizar acompanhamento
            </button>
          )}
          <button type="button" disabled={enviando} onClick={onVoltar} className="rounded-lg border border-[#dfe6e7] px-3 py-2 font-semibold text-[#17695b] disabled:opacity-60">
            {somenteReenvio ? "Voltar ao acompanhamento" : textos.voltar}
          </button>
        </aside>
      </div>
    </>
  );
}
