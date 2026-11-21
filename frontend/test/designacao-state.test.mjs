import assert from "node:assert/strict";
import { test } from "node:test";
import {
  concluirConsulta,
  falharConsulta,
  iniciarConsulta,
} from "../src/features/designacao/state.ts";

const consulta = id => ({
  freteId: Number(id), solicitacaoId: Number(id), modalidade: "PESSOA",
  status: "AGUARDANDO_DESIGNACAO", dataPretendida: "2030-01-01T08:00:00",
  origem: "Origem", destino: "Destino", valor: 100, condicoes: null,
  revisao: 0, quantidadePessoas: 1, pesoEstimadoKg: null,
  prestadorAutonomo: false, designacaoAtual: null, motoristas: [], veiculos: [],
});

test("descarta a resposta atrasada quando outro frete já foi aberto", () => {
  const primeiro = iniciarConsulta("10");
  const segundo = iniciarConsulta("20");
  assert.deepEqual(concluirConsulta(segundo, "10", consulta("10")), segundo);
  assert.deepEqual(concluirConsulta(primeiro, "10", consulta("10")), {
    tipo: "pronto", freteId: "10", dados: consulta("10"),
  });
});

test("representa carregamento, vazio, erro e conflito sem conservar dados anteriores", () => {
  const carregando = iniciarConsulta("30");
  assert.deepEqual(carregando, { tipo: "carregando", freteId: "30" });
  assert.deepEqual(concluirConsulta(carregando, "30", null), {
    tipo: "vazio", freteId: "30",
  });
  assert.deepEqual(falharConsulta(carregando, "30", "Falhou", 500), {
    tipo: "erro", freteId: "30", mensagem: "Falhou", conflito: false,
  });
  assert.deepEqual(falharConsulta(carregando, "30", "Atualize", 409), {
    tipo: "erro", freteId: "30", mensagem: "Atualize", conflito: true,
  });
});
