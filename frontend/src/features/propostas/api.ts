export type Avaliacao = {
  id: number;
  modalidade: "PESSOA" | "ENCOMENDA";
  status: string;
  cliente: string;
  partida: string;
  origem: string;
  destino: string;
  passageiros: number | null;
  bagagens: string | null;
  volumes: string | null;
  pesoKg: number | null;
  coleta: string | null;
  entrega: string | null;
  valorCliente: number | null;
  condicoesCliente: string | null;
  justificativaRecusa: string | null;
  propostaEnviada: boolean;
  mapaCaminho: string | null;
  percursoKm: number | null;
  deslocamentoMinutos: number | null;
  duracaoMinutos: number | null;
  paradas: { endereco: string; horario: string | null }[];
};

type Coordenada = { latitude: number; longitude: number };
export type Rota = {
  pontos: { endereco: string; horario: string | null; coordenada: Coordenada | null }[];
  geometria: Coordenada[];
};

export async function obterPercurso(id: string, signal: AbortSignal): Promise<Rota> {
  const res = await fetch(base(id) + "/avaliacao/percurso", { signal, cache: "no-store" });
  await conferir(res);
  return res.json();
}

function base(id: string) {
  return "/api/prestador/solicitacoes/" + encodeURIComponent(id);
}

export class ConflitoProposta extends Error {}

async function conferir(res: Response) {
  if (res.ok) return;
  const mensagens: Record<number, string> = {
    400: "Confira o valor e a justificativa.",
    401: "Entre novamente para responder.",
    403: "Sua sessão ou permissão não permite esta operação.",
    404: "Solicitação indisponível.",
    409: "A solicitação foi encerrada ou a oferta mudou. Atualize a avaliação.",
  };
  if (res.status === 409) throw new ConflitoProposta(mensagens[409]);
  throw new Error(
    mensagens[res.status] ??
      "Não foi possível acessar a solicitação. Tente novamente.",
  );
}

export async function avaliar(
  id: string,
  signal?: AbortSignal,
): Promise<Avaliacao> {
  const res = await fetch(base(id) + "/avaliacao", {
    signal,
    cache: "no-store",
  });
  await conferir(res);
  return res.json();
}

export async function enviar(id: string, recusa: boolean, resposta: object) {
  const csrf = await fetch("/api/auth/csrf", { cache: "no-store" });
  await conferir(csrf);
  const { headerName, token } = await csrf.json();
  const res = await fetch(base(id) + (recusa ? "/recusa" : "/propostas"), {
    method: "POST",
    headers: { "Content-Type": "application/json", [headerName]: token },
    body: JSON.stringify(resposta),
  });
  await conferir(res);
}
