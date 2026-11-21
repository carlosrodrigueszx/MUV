export type ModalidadeFrete = "PESSOA" | "ENCOMENDA";

export interface DesignacaoAtual {
  motoristaId: number | null;
  motoristaNome: string | null;
  veiculoId: number | null;
  veiculoMarca: string | null;
  veiculoModelo: string | null;
  veiculoPlaca: string | null;
}

export interface MotoristaDisponivel {
  id: number;
  nome: string;
}

export interface VeiculoDisponivel {
  id: number;
  marca: string;
  modelo: string;
  ano: number;
  placa: string;
  cor: string;
  modalidade: ModalidadeFrete | "AMBOS";
  ativo: boolean;
  capacidadePassageiros: number;
  capacidadeCargaKg: number;
  compativel: boolean;
  incompatibilidades: string[];
}

export interface ConsultaDesignacao {
  freteId: number;
  solicitacaoId: number;
  modalidade: ModalidadeFrete;
  status: string;
  dataPretendida: string;
  origem: string;
  destino: string;
  valor: number;
  condicoes: string | null;
  revisao: number;
  quantidadePessoas: number | null;
  pesoEstimadoKg: number | null;
  prestadorAutonomo: boolean;
  designacaoAtual: DesignacaoAtual | null;
  motoristas: MotoristaDisponivel[];
  veiculos: VeiculoDisponivel[];
}

export interface DesignacaoConfirmada {
  freteId: number;
  status: string;
  revisao: number;
  designacaoAtual: DesignacaoAtual;
}

export interface SubstituicaoConfirmada extends DesignacaoConfirmada {
  solicitacaoAcordoId: number | null;
}

export interface ComandoSubstituicao {
  motoristaId: number;
  veiculoId: number;
  justificativa: string;
  revisao: number;
  novoValor?: number;
  novasCondicoes?: string;
}
