-- RF009: controle concorrente da designação e suporte auditável a alterações.
ALTER TABLE frete
    ADD COLUMN revisao_designacao BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT frete_revisao_designacao_ck CHECK (revisao_designacao >= 0);

CREATE SEQUENCE solicitacao_alteracao_acordo_id_seq
    OWNED BY solicitacao_alteracao_acordo.id;
SELECT setval(
    'solicitacao_alteracao_acordo_id_seq',
    COALESCE((SELECT MAX(id) FROM solicitacao_alteracao_acordo), 0) + 1,
    false
);
ALTER TABLE solicitacao_alteracao_acordo
    ALTER COLUMN id SET DEFAULT nextval('solicitacao_alteracao_acordo_id_seq'),
    ALTER COLUMN status SET DEFAULT 'PENDENTE',
    ADD COLUMN revisao_origem BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT solicitacao_alteracao_status_ck
        CHECK (status IN ('PENDENTE', 'ACEITA', 'REJEITADA')),
    ADD CONSTRAINT solicitacao_alteracao_revisao_ck
        CHECK (revisao_origem >= 0),
    ADD CONSTRAINT solicitacao_alteracao_justificativa_ck
        CHECK (CHAR_LENGTH(TRIM(justificativa)) > 0),
    ADD CONSTRAINT solicitacao_alteracao_resposta_ck
        CHECK ((status = 'PENDENTE' AND data_hora_resposta IS NULL)
            OR (status IN ('ACEITA', 'REJEITADA') AND data_hora_resposta IS NOT NULL));

CREATE UNIQUE INDEX solicitacao_alteracao_pendente_por_frete_uk
    ON solicitacao_alteracao_acordo (id_frete)
    WHERE status = 'PENDENTE';

CREATE SEQUENCE historico_execucao_frete_id_seq
    OWNED BY historico_execucao_frete.id;
SELECT setval(
    'historico_execucao_frete_id_seq',
    COALESCE((SELECT MAX(id) FROM historico_execucao_frete), 0) + 1,
    false
);
ALTER TABLE historico_execucao_frete
    ALTER COLUMN id SET DEFAULT nextval('historico_execucao_frete_id_seq');
