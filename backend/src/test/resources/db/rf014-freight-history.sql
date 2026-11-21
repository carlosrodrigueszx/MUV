INSERT INTO usuario (id, nome, telefone, email, senha, criado_em) VALUES
    (914001, 'Cliente RF014', '85914000001', 'cliente-rf014@example.test', 'senha-de-teste', TIMESTAMP '2026-11-01 08:00:00'),
    (914002, 'Prestador Alfa RF014', '85914000002', 'prestador-alfa-rf014@example.test', 'senha-de-teste', TIMESTAMP '2026-11-01 08:00:00'),
    (914003, 'Prestador Beta RF014', '85914000003', 'prestador-beta-rf014@example.test', 'senha-de-teste', TIMESTAMP '2026-11-01 08:00:00'),
    (914004, 'Outro Cliente RF014', '85914000004', 'outro-cliente-rf014@example.test', 'senha-de-teste', TIMESTAMP '2026-11-01 08:00:00');

INSERT INTO passageiro (id, cpf) VALUES
    (914001, '914.000.001-01'),
    (914004, '914.000.004-04');

INSERT INTO prestador (id, avaliacao_media, total_avaliacoes) VALUES
    (914002, 4.75, 12),
    (914003, 4.25, 8);

INSERT INTO motorista (id, nome, telefone, cnh, categoria_cnh) VALUES
    (914401, 'Motorista RF014', '85914400401', 'CNH-RF014-01', 'D');

INSERT INTO vinculo_motorista
    (prestador_id, motorista_id, status_verificacao, verificado_por_id, data_verificacao)
VALUES
    (914002, 914401, 'APROVADO', 914002, TIMESTAMP '2026-11-02 09:00:00');

INSERT INTO veiculo
    (id, modelo, ano, cor, placa, capacidade_passageiros, capacidade_carga_kg,
     marca, modalidade, ativo, id_prestador)
VALUES
    (914501, 'Master', 2024, 'Branco', 'RF01401', 15, 1200.00,
     'Renault', 'AMBOS', TRUE, 914002);

INSERT INTO endereco
    (id, cep, logradouro, numero, bairro, cidade, estado)
VALUES
    (914011, '01001-000', 'Rua Origem Um', '10', 'Centro', 'São Paulo', 'SP'),
    (914012, '11010-000', 'Rua Destino Um', '20', 'Centro', 'Santos', 'SP'),
    (914013, '13010-000', 'Rua Origem Dois', '30', 'Centro', 'Campinas', 'SP'),
    (914014, '18010-000', 'Rua Destino Dois', '40', 'Centro', 'Sorocaba', 'SP');

INSERT INTO solicitacao_frete
    (id, modalidade, data_hora_pretendida, status, criado_em, id_passageiro,
     id_prestador, endereco_origem_id, endereco_destino_id)
VALUES
    (914101, 'ENCOMENDA', TIMESTAMP '2026-11-15 10:00:00', 'ABERTA',
     TIMESTAMP '2026-11-10 09:00:00', 914001, NULL, 914011, 914012),
    (914102, 'PESSOA', TIMESTAMP '2026-11-18 08:00:00', 'CONTRATADA',
     TIMESTAMP '2026-11-11 09:00:00', 914001, 914002, 914013, 914014),
    (914103, 'ENCOMENDA', TIMESTAMP '2026-11-25 14:00:00', 'ABERTA',
     TIMESTAMP '2026-11-12 09:00:00', 914004, NULL, 914013, 914014);

INSERT INTO solicitacao_encomenda
    (id, descricao_volumes, peso_estimado_kg, altura_cm, largura_cm, profundidade_cm,
     nome_contato_coleta, telefone_contato_coleta, nome_contato_entrega,
     telefone_contato_entrega)
VALUES
    (914101, '12 caixas de teste', 180.00, 80.00, 100.00, 120.00,
     'Coleta RF014', '85914000101', 'Entrega RF014', '85914000102'),
    (914103, '1 volume de outro cliente', 20.00, 30.00, 40.00, 50.00,
     'Outra Coleta RF014', '85914000103', 'Outra Entrega RF014', '85914000104');

INSERT INTO solicitacao_pessoa
    (id, quantidade_pessoas, possui_bagagem, descricao_bagagens)
VALUES
    (914102, 4, TRUE, 'Quatro malas');

INSERT INTO proposta
    (id, status, valor, condicoes, criado_em, id_solicitacao, id_prestador)
VALUES
    (914201, 'ENVIADA', 480.00, 'Entrega até 12:00.', TIMESTAMP '2026-11-10 10:00:00', 914101, 914002),
    (914202, 'ENCERRADA', 510.00, 'Entrega até 13:00.', TIMESTAMP '2026-11-10 11:00:00', 914101, 914003),
    (914203, 'ACEITA', 740.00, 'Viagem direta.', TIMESTAMP '2026-11-11 10:00:00', 914102, 914002),
    (914204, 'ENVIADA', 320.00, 'Coleta no período da tarde.', TIMESTAMP '2026-11-12 10:00:00', 914103, 914003);

INSERT INTO frete
    (id, valor_atual, condicoes_atuais, status, criado_em, solicitacao_id,
     proposta_aceita_id, prestador_responsavel_id, motorista_atual_id,
     veiculo_atual_id, data_hora_conclusao, responsavel_registro_conclusao_id)
VALUES
    (914301, 740.00, 'Viagem direta.', 'CONCLUIDO', TIMESTAMP '2026-11-12 08:00:00',
     914102, 914203, 914002, 914401, 914501, TIMESTAMP '2026-11-20 11:15:00', 914002);

INSERT INTO historico_execucao_frete
    (id, frete_id, usuario_autor_id, tipo_evento, descricao, motorista_id,
     veiculo_id, data_hora)
VALUES
    (914601, 914301, 914002, 'INICIO', 'Saída confirmada.', 914401, 914501,
     TIMESTAMP '2026-11-20 08:00:00'),
    (914602, 914301, 914002, 'CONCLUSAO', 'Chegada confirmada.', 914401, 914501,
     TIMESTAMP '2026-11-20 11:15:00');
