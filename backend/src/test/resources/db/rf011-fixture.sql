-- Somente para schema/banco isolado de testes RF011. IDs e contas sintéticos.
TRUNCATE usuario, endereco RESTART IDENTITY CASCADE;
INSERT INTO usuario (id,nome,telefone,email,senha) VALUES
 (11001,'Cliente RF011','85900000001','cliente.rf011@example.test','test'),
 (11002,'Prestador RF011','85900000002','prestador.rf011@example.test','test'),
 (11003,'Terceiro RF011','85900000003','terceiro.rf011@example.test','test');
INSERT INTO passageiro(id,cpf) VALUES (11001,'00000011001');
INSERT INTO prestador(id) VALUES (11002);
INSERT INTO motorista(id,nome,telefone,cnh) VALUES (11004,'Motorista RF011','85900000004','01100000004');
INSERT INTO vinculo_motorista(prestador_id,motorista_id) VALUES (11002,11004);
INSERT INTO veiculo(id,modelo,ano,cor,placa,marca,modalidade,id_prestador)
 VALUES (11005,'Teste',2026,'Branco','RFT1A11','Teste','ENCOMENDA',11002);
INSERT INTO endereco(id,cep,logradouro,numero,bairro,cidade,estado) VALUES
 (11006,'60000000','Origem','1','Teste','São Paulo','SP'),
 (11007,'60000000','Destino','2','Teste','Santos','SP');
INSERT INTO solicitacao_frete(id,modalidade,data_hora_pretendida,status,id_passageiro,endereco_origem_id,endereco_destino_id)
 VALUES (11008,'ENCOMENDA','2026-11-23 08:00:00','CONTRATADA',11001,11006,11007);
INSERT INTO proposta(id,status,valor,condicoes,id_solicitacao,id_prestador)
 VALUES (11009,'ACEITA',350.00,'Condições originais',11008,11002);
INSERT INTO frete(id,valor_atual,condicoes_atuais,status,solicitacao_id,proposta_aceita_id,
 prestador_responsavel_id,motorista_atual_id,veiculo_atual_id,criado_em)
 VALUES (11010,350.00,'Condições originais','PRONTO_INICIO',11008,11009,11002,11004,11005,'2026-10-21 14:00:00');
