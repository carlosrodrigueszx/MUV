export function duracao(minutos: number) {
  const horas = Math.floor(minutos / 60);
  const resto = minutos % 60;
  return horas
    ? resto
      ? horas + "h" + String(resto).padStart(2, "0")
      : horas + " h"
    : resto + " min";
}
