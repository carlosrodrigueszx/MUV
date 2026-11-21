import { useEffect, useRef } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { LuRoute } from "react-icons/lu";
import type { Avaliacao, Rota } from "./api";
import { duracao } from "./duracao";

export function Percurso({ dados, rota }: { dados: Avaliacao; rota: Rota | null | undefined }) {
  const container = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!container.current || !rota) return;
    const mapa = L.map(container.current, { zoomControl: false });
    L.control.zoom({ position: "topright", zoomInTitle: "Ampliar mapa", zoomOutTitle: "Reduzir mapa" }).addTo(mapa);
    L.tileLayer(import.meta.env.VITE_RF007_TILES || "https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 19,
    }).addTo(mapa);
    const limites = L.latLngBounds([]);
    if (rota.geometria.length) {
      const linha = rota.geometria.map(c => L.latLng(c.latitude, c.longitude));
      L.polyline(linha, { color: "white", weight: 8 }).addTo(mapa);
      limites.extend(L.polyline(linha, { color: "#17695b", weight: 5, className: "rf-rota" }).addTo(mapa).getBounds());
    }
    rota.pontos.forEach((ponto, i) => {
      if (!ponto.coordenada) return;
      const pos = L.latLng(ponto.coordenada.latitude, ponto.coordenada.longitude);
      const rotulo = i === 0 ? "A" : i === rota.pontos.length - 1 ? "B" : String(i);
      L.marker(pos, {
        title: ponto.endereco,
        icon: L.divIcon({ className: "rf-pin", html: rotulo, iconSize: [24, 24], iconAnchor: [12, 12] }),
      }).addTo(mapa);
      limites.extend(pos);
    });
    if (limites.isValid()) mapa.fitBounds(limites, { padding: [32, 36], maxZoom: 16 });
    else mapa.fitBounds([[-34, -74], [6, -34]]);
    const resize = new ResizeObserver(() => mapa.invalidateSize());
    resize.observe(container.current);
    return () => { resize.disconnect(); mapa.remove(); };
  }, [rota]);
  return (
    <section className="rf-mapa" aria-label="Percurso da solicitação">
      <div ref={container} className="rf-cartografia" />
      {!rota?.geometria.length && <div className="rf-mapa-aviso">
        {rota === undefined ? "Carregando percurso…" : "Traçado viário indisponível."}
      </div>}
      {(dados.percursoKm != null || dados.deslocamentoMinutos != null) && (
        <div className="rf-legenda">
          <LuRoute />
          {[
            dados.percursoKm != null &&
              dados.percursoKm.toLocaleString("pt-BR") + " km",
            dados.deslocamentoMinutos != null &&
              duracao(dados.deslocamentoMinutos) + " de deslocamento",
          ]
            .filter(Boolean)
            .join(" · ")}
        </div>
      )}
    </section>
  );
}
