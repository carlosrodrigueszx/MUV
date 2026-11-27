-- RF010 guarda a confirmação sem mudar o contrato de leitura do histórico.
CREATE TABLE rf010_recibo_inicio (
    frete_id BIGINT NOT NULL REFERENCES frete(id),
    chave_operacao UUID NOT NULL,
    evento_id BIGINT NOT NULL UNIQUE REFERENCES historico_execucao_frete(id),
    revisao_designacao BIGINT NOT NULL CHECK (revisao_designacao >= 0),
    estado_anterior VARCHAR(28) NOT NULL CHECK (estado_anterior = 'PRONTO_INICIO'),
    estado_resultante VARCHAR(28) NOT NULL CHECK (estado_resultante = 'EM_EXECUCAO'),
    PRIMARY KEY (frete_id, chave_operacao)
);
