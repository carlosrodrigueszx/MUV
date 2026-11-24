UPDATE solicitacao_frete
SET data_hora_pretendida = CURRENT_TIMESTAMP + INTERVAL '10 days'
WHERE id = 910041;

UPDATE solicitacao_frete
SET data_hora_pretendida = CURRENT_TIMESTAMP - INTERVAL '1 day'
WHERE id IN (910042, 910043);

-- Quatro partidas anteriores à do frete 910061 ocupam toda a agenda.
INSERT INTO solicitacao_frete(id, modalidade, data_hora_pretendida, status, id_passageiro,
    endereco_origem_id, endereco_destino_id, valor_cliente, condicoes_cliente)
SELECT 912000 + n, 'PESSOA', CURRENT_TIMESTAMP + n * INTERVAL '1 day', 'CONTRATADA',
    910001, 910032, 910031, 100, 'Agenda RF010'
FROM generate_series(1, 4) n;

INSERT INTO solicitacao_pessoa(id, quantidade_pessoas, possui_bagagem)
SELECT 912000 + n, 4, FALSE FROM generate_series(1, 4) n;

INSERT INTO proposta(id, status, valor, condicoes, id_solicitacao, id_prestador)
SELECT 912010 + n, 'ACEITA', 100, 'Agenda RF010', 912000 + n, 910002
FROM generate_series(1, 4) n;

INSERT INTO frete(id, valor_atual, condicoes_atuais, status, solicitacao_id, proposta_aceita_id,
    prestador_responsavel_id, motorista_atual_id, veiculo_atual_id, revisao_designacao)
SELECT 912020 + n, 100, 'Agenda RF010', 'PRONTO_INICIO', 912000 + n, 912010 + n,
    910002, 910011, 910021, 1
FROM generate_series(1, 4) n;
