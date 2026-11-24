CREATE SEQUENCE endereco_id_seq OWNED BY endereco.id;
SELECT setval('endereco_id_seq', COALESCE(MAX(id), 0) + 1, false) FROM endereco;
ALTER TABLE endereco ALTER COLUMN id SET DEFAULT nextval('endereco_id_seq');

CREATE SEQUENCE solicitacao_frete_id_seq OWNED BY solicitacao_frete.id;
SELECT setval('solicitacao_frete_id_seq', COALESCE(MAX(id), 0) + 1, false) FROM solicitacao_frete;
ALTER TABLE solicitacao_frete
    ALTER COLUMN id SET DEFAULT nextval('solicitacao_frete_id_seq'),
    ADD COLUMN retorno_previsto TIMESTAMP,
    ADD COLUMN chave_envio UUID,
    ADD UNIQUE (id_passageiro, chave_envio),
    ADD CHECK (retorno_previsto IS NULL OR retorno_previsto > data_hora_pretendida);
