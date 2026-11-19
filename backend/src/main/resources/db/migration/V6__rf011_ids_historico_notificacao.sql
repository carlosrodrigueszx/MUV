CREATE SEQUENCE historico_execucao_frete_id_seq
    OWNED BY Historico_Execucao_Frete.id;

SELECT setval(
    'historico_execucao_frete_id_seq',
    COALESCE(MAX(id), 0) + 1,
    false
)
FROM Historico_Execucao_Frete;

ALTER TABLE Historico_Execucao_Frete
    ALTER COLUMN id SET DEFAULT nextval('historico_execucao_frete_id_seq');

CREATE SEQUENCE notificacao_id_seq
    OWNED BY Notificacao.id;

SELECT setval(
    'notificacao_id_seq',
    COALESCE(MAX(id), 0) + 1,
    false
)
FROM Notificacao;

ALTER TABLE Notificacao
    ALTER COLUMN id SET DEFAULT nextval('notificacao_id_seq');
