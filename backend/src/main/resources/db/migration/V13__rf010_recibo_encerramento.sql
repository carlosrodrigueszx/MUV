-- A situação da encomenda fica no RF010, fora da leitura geral do histórico.
CREATE TABLE rf010_recibo_encerramento (
    frete_id BIGINT NOT NULL REFERENCES frete(id),
    chave_operacao UUID NOT NULL,
    evento_id BIGINT NOT NULL UNIQUE REFERENCES historico_execucao_frete(id),
    interrupcao_id BIGINT NOT NULL UNIQUE REFERENCES historico_execucao_frete(id),
    revisao_designacao BIGINT NOT NULL CHECK (revisao_designacao >= 0),
    estado_anterior VARCHAR(28) NOT NULL CHECK (estado_anterior = 'INTERROMPIDO'),
    estado_resultante VARCHAR(28) NOT NULL CHECK (estado_resultante = 'ENCERRADO_SEM_CONCLUSAO'),
    data_hora_informada TIMESTAMP NOT NULL,
    situacao_encomenda TEXT,
    PRIMARY KEY (frete_id, chave_operacao)
);
