-- Dados opcionais da solicitação. Registros anteriores continuam legíveis.
ALTER TABLE solicitacao_frete
    ADD COLUMN mapa_caminho TEXT,
    ADD COLUMN percurso_km NUMERIC(10,2) CHECK (percurso_km >= 0),
    ADD COLUMN deslocamento_minutos INTEGER CHECK (deslocamento_minutos >= 0),
    ADD COLUMN duracao_minutos INTEGER CHECK (duracao_minutos >= 0);
