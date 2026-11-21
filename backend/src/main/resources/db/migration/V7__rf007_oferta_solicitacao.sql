ALTER TABLE solicitacao_frete
    ADD COLUMN valor_cliente NUMERIC(13,2) CHECK (valor_cliente > 0),
    ADD COLUMN condicoes_cliente TEXT;

ALTER TABLE proposta ADD COLUMN chave_envio UUID;
CREATE UNIQUE INDEX proposta_envio_unico ON proposta (id_prestador, id_solicitacao, chave_envio);

CREATE SEQUENCE rf007_proposta_id_seq OWNED BY proposta.id;
SELECT setval('rf007_proposta_id_seq', COALESCE(MAX(id), 0) + 1, false) FROM proposta;
ALTER TABLE proposta ALTER COLUMN id SET DEFAULT nextval('rf007_proposta_id_seq');

CREATE TABLE recusa_solicitacao (
    id_solicitacao BIGINT REFERENCES solicitacao_frete(id),
    id_prestador BIGINT REFERENCES prestador(id),
    justificativa TEXT NOT NULL CHECK (trim(justificativa) <> ''),
    PRIMARY KEY (id_solicitacao, id_prestador)
);

CREATE TABLE solicitacao_parada (
    id_solicitacao BIGINT REFERENCES solicitacao_frete(id),
    ordem INTEGER NOT NULL,
    endereco_id BIGINT NOT NULL REFERENCES endereco(id),
    horario TIMESTAMP,
    PRIMARY KEY (id_solicitacao, ordem)
);

DO $$
BEGIN
IF pg_get_serial_sequence('notificacao', 'id') IS NULL THEN
CREATE SEQUENCE rf007_notificacao_id_seq OWNED BY notificacao.id;
PERFORM setval('rf007_notificacao_id_seq', COALESCE(MAX(id), 0) + 1, false) FROM notificacao;
ALTER TABLE notificacao ALTER COLUMN id SET DEFAULT nextval('rf007_notificacao_id_seq');
END IF;
END $$;
