import assert from "node:assert/strict";
import { test } from "node:test";
import { validateTrip } from "../src/features/trip-planning/criteria.ts";

const now = new Date(2030, 5, 1, 8);

function tripForm(overrides = {}) {
  const form = new FormData();
  for (const [field, value] of Object.entries({
    embarque: " Fortaleza ",
    destino: "Natal",
    dataIda: "2030-06-02",
    horaIda: "08:00",
    passageiros: "6",
    ...overrides,
  })) {
    if (value !== undefined) form.set(field, value);
  }
  return form;
}

function assertInvalid(form, field, roundTrip = false) {
  const result = validateTrip(form, roundTrip, now);
  assert.equal(result.field, field);
  assert.ok(result.error);
  assert.equal(result.trip, undefined);
}

test("aceita ida sem oferta e ignora o retorno quando a modalidade é ida", () => {
  const form = tripForm({ dataRetorno: "2030-06-03", horaRetorno: "17:00" });
  assert.deepEqual(validateTrip(form, false, now), {
    trip: {
      origin: "Fortaleza",
      destination: "Natal",
      stops: [],
      departure: "2030-06-02T08:00",
      passengers: 6,
    },
  });
});

test("mantém retorno, paradas ordenadas e os dois campos da oferta", () => {
  const form = tripForm({
    dataRetorno: "2030-06-03", horaRetorno: "17:00",
    valor: ".50", condicoes: " Inclui pedágios ",
  });
  form.append("parada", " Aracati ");
  form.append("parada", "Mossoró");
  const { trip } = validateTrip(form, true, now);
  assert.equal(trip.returnAt, "2030-06-03T17:00");
  assert.deepEqual(trip.stops, ["Aracati", "Mossoró"]);
  assert.deepEqual(trip.offer, { amount: ".50", terms: "Inclui pedágios" });
});

test("rejeita origem, destino ou parada em branco", () => {
  for (const field of ["embarque", "destino", "parada"]) {
    assertInvalid(tripForm({ [field]: "   " }), field);
  }
});

test("rejeita partida ausente, passada ou igual ao instante atual", () => {
  for (const date of [undefined, "2030-05-31", "2030-06-01"]) {
    assertInvalid(tripForm({ dataIda: date }), "dataIda");
  }
});

test("exige retorno estritamente posterior à partida em ida e volta", () => {
  assertInvalid(tripForm(), "dataRetorno", true);
  for (const time of ["07:59", "08:00"]) {
    assertInvalid(tripForm({ dataRetorno: "2030-06-02", horaRetorno: time }), "dataRetorno", true);
  }
});

test("exige quantidade inteira, positiva e representável no backend", () => {
  for (const value of [undefined, "0", "-1", "1.5", "2147483648"]) {
    assertInvalid(tripForm({ passageiros: value }), "passageiros");
  }
});

test("rejeita oferta parcial, não positiva ou com precisão inválida", () => {
  assertInvalid(tripForm({ condicoes: "Inclui pedágios" }), "valor");
  assertInvalid(tripForm({ valor: "250", condicoes: "  " }), "condicoes");
  for (const value of ["0", "-1", "10.001", "100000000000"]) {
    assertInvalid(tripForm({ valor: value, condicoes: "Inclui pedágios" }), "valor");
  }
});
