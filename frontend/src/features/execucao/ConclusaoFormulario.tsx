import { useRef, useState } from "react";
import { concluirFrete, type Acompanhamento } from "./api";
import { focarAoMontar } from "./foco";

const controle = "h-11 min-w-0 rounded-lg border border-slate-300 px-3 focus-visible:outline-2 focus-visible:outline-[#17695b]";

const relogioFortaleza = new Intl.DateTimeFormat("pt-BR", {
  timeZone: "America/Fortaleza",
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  hourCycle: "h23",
});

function agoraEmFortaleza() {
  const partes = Object.fromEntries(relogioFortaleza.formatToParts(new Date())
    .map(({ type, value }) => [type, value]));
  return `${partes.year}-${partes.month}-${partes.day}T${partes.hour}:${partes.minute}`;
}

export function ConclusaoFormulario({ dados, onVoltar, onConcluido }: {
  dados: Acompanhamento;
  onVoltar: () => void;
  onConcluido: () => void;
}) {
  const pendente = useRef(false);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState("");
  const encomenda = dados.modalidade === "ENCOMENDA";

  return <form className="max-w-2xl space-y-4 rounded-xl border border-[#dfe6e7] bg-white p-5"
    onSubmit={async event => {
      event.preventDefault();
      if (pendente.current) return;
      const formulario = new FormData(event.currentTarget);
      const valor = (chave: string) => String(formulario.get(chave) ?? "").trim();
      const data = valor("data");
      const horario = valor("horario");
      if (encomenda && `${data}T${horario}` > agoraEmFortaleza()) {
        setErro("A data e o horário da entrega não podem estar no futuro. Use o horário de Fortaleza.");
        return;
      }
      pendente.current = true;
      setEnviando(true);
      setErro("");
      try {
        await concluirFrete(dados.freteId, {
          revisaoDesignacao: dados.revisaoDesignacao,
          cicloExecucaoId: dados.cicloExecucaoId,
          ...(encomenda && {
            data, horario, nomeRecebedor: valor("nomeRecebedor"),
            cpfRecebedor: valor("cpfRecebedor") || undefined,
          }),
        });
        onConcluido();
      } catch (falha) {
        setErro(falha instanceof TypeError || (falha instanceof DOMException && falha.name === "TimeoutError")
          ? "A conexão com o servidor falhou. Tente novamente para confirmar o registro."
          : falha instanceof Error ? falha.message : "Não foi possível registrar a conclusão.");
      } finally {
        pendente.current = false;
        setEnviando(false);
      }
    }}>
    <h1 tabIndex={-1} ref={focarAoMontar} className="text-xl font-bold">Concluir frete FR-{dados.freteId}</h1>
    <p className="text-sm text-[#637479]">Ao confirmar, o frete será concluído e o cliente será notificado.</p>
    {encomenda && <fieldset disabled={enviando} aria-describedby="orientacao-entrega" className="grid gap-4 sm:grid-cols-2">
      <legend className="mb-3 font-semibold">Entrega e recebedor</legend>
      <p id="orientacao-entrega" className="text-sm text-[#637479] sm:col-span-2">
        Informe quando a entrega aconteceu, no horário de Fortaleza.
      </p>
      <label className="grid gap-1 text-sm">Data da entrega
        <input name="data" type="date" className={controle} required />
      </label>
      <label className="grid gap-1 text-sm">Horário da entrega (Fortaleza)
        <input name="horario" type="time" className={controle} required />
      </label>
      <label className="grid gap-1 text-sm">Nome de quem recebeu
        <input name="nomeRecebedor" className={controle} maxLength={128} required />
      </label>
      <label className="grid gap-1 text-sm">CPF de quem recebeu (opcional)
        <input name="cpfRecebedor" className={controle} maxLength={14} inputMode="numeric" />
      </label>
    </fieldset>}
    {erro && <p role="alert" tabIndex={-1} ref={focarAoMontar} className="rounded-lg border border-red-200 bg-red-50 p-3 text-red-800">{erro}</p>}
    <div className="flex flex-wrap gap-3">
      <button disabled={enviando} className="min-h-12 rounded-lg bg-[#17695b] px-4 text-base font-semibold text-white enabled:cursor-pointer enabled:hover:bg-[#12584d] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#17695b] disabled:opacity-60">
        {enviando ? "Registrando…" : "Confirmar conclusão"}
      </button>
      <button type="button" disabled={enviando} onClick={onVoltar} className="min-h-12 rounded-lg border border-slate-500 px-4 text-base font-semibold enabled:cursor-pointer enabled:hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#17695b]">
        Voltar ao acompanhamento
      </button>
    </div>
  </form>;
}
