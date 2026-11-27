export interface TripCriteria {
  origin: string;
  destination: string;
  stops: string[];
  departure: string;
  returnAt?: string;
  passengers: number;
  offer?: { amount: string; terms: string };
}

export interface TripNavigationState {
  trip?: TripCriteria;
  prestadorId?: number;
}

export function validateTrip(form: FormData, roundTrip: boolean, now = new Date()) {
  const value = (name: string) => String(form.get(name) ?? "").trim();
  const origin = value("embarque");
  const destination = value("destino");
  const stops = form.getAll("parada").map(stop => String(stop).trim());
  const departure = `${value("dataIda")}T${value("horaIda")}`;
  const returnAt = roundTrip ? `${value("dataRetorno")}T${value("horaRetorno")}` : undefined;
  const passengers = Number(value("passageiros"));
  const amount = value("valor");
  const terms = value("condicoes");

  if (!origin) return { error: "Informe o ponto de embarque.", field: "embarque" } as const;
  if (!destination) return { error: "Informe o destino.", field: "destino" } as const;
  if (stops.includes("")) return { error: "Informe o local de cada parada ou remova a parada vazia.", field: "parada" } as const;
  const departureTime = new Date(departure).getTime();
  if (!Number.isFinite(departureTime) || departureTime <= now.getTime()) {
    return { error: "Informe uma data e um horário de ida futuros.", field: "dataIda" } as const;
  }
  if (returnAt && !(new Date(returnAt).getTime() > departureTime)) {
    return { error: "O retorno deve ocorrer depois da partida.", field: "dataRetorno" } as const;
  }
  if (!Number.isInteger(passengers) || passengers < 1 || passengers > 2147483647) {
    return { error: "Informe uma quantidade inteira e positiva de passageiros.", field: "passageiros" } as const;
  }
  if (amount || form.get("condicoes")) {
    if (!/^(?:\d+(?:\.\d{1,2})?|\.\d{1,2})$/.test(amount) ||
        Number(amount) <= 0 || Number(amount) > 99999999999.99) {
      return { error: "Informe um valor positivo em reais com até duas casas decimais.", field: "valor" } as const;
    }
    if (!terms) return { error: "Informe as condições da oferta.", field: "condicoes" } as const;
  }

  const trip: TripCriteria = { origin, destination, stops, departure, passengers };
  if (returnAt) trip.returnAt = returnAt;
  if (amount) trip.offer = { amount, terms };
  return { trip } as const;
}
