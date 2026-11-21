INSERT INTO usuario(id,nome,telefone,email,senha,is_admin) VALUES
(909001,'Cliente RF009','85909090001','cliente909@example.test','fixture',false),
(909002,'Empresa RF009','85909090002','empresa909@example.test','fixture',false),
(909003,'Motorista Verificado','85909090003','verificado909@example.test','fixture',false),
(909004,'Motorista Pendente','85909090004','pendente909@example.test','fixture',false),
(909005,'Motorista Rejeitado','85909090005','rejeitado909@example.test','fixture',false),
(909006,'Outra Empresa RF009','85909090006','outra909@example.test','fixture',false),
(909007,'Motorista Externo','85909090007','externo909@example.test','fixture',false),
(909008,'Autonomo sem Motorista','85909090008','autonomo909@example.test','fixture',false),
(909009,'Motorista Verificado Dois','85909090009','verificado2-909@example.test','fixture',false);

INSERT INTO passageiro(id,cpf) VALUES (909001,'909.000.001-01');
INSERT INTO prestador(id) VALUES (909002),(909006),(909008);
INSERT INTO prestador_empresa(id,cnpj,razao_social,nome_fantasia) VALUES
(909002,'90900000000102','Empresa RF009 Ltda','Empresa RF009'),
(909006,'90900000000106','Outra Empresa RF009 Ltda','Outra Empresa RF009');
INSERT INTO prestador_autonomo(id,cpf) VALUES (909008,'909.000.008-08');

INSERT INTO motorista(id,nome,telefone,cnh,categoria_cnh,usuario_id) VALUES
(909010,'Motorista Verificado','85909090003','90900000010','D',909003),
(909011,'Motorista Pendente','85909090004','90900000011','D',909004),
(909012,'Motorista Rejeitado','85909090005','90900000012','D',909005),
(909013,'Motorista Externo','85909090007','90900000013','D',909007),
(909018,'Motorista Verificado Dois','85909090009','90900000018','D',909009);

INSERT INTO vinculo_motorista(prestador_id,motorista_id,status_verificacao,verificado_por_id,data_verificacao) VALUES
(909002,909010,'VERIFICADO',909002,CURRENT_TIMESTAMP),
(909002,909011,'PENDENTE',NULL,NULL),
(909002,909012,'REJEITADO',909002,CURRENT_TIMESTAMP),
(909006,909013,'VERIFICADO',909006,CURRENT_TIMESTAMP),
(909002,909018,'VERIFICADO',909002,CURRENT_TIMESTAMP);

INSERT INTO autorizacao_modalidade(id,modalidade,status,id_prestador) VALUES
(909014,'PESSOA','APROVADO',909002),
(909015,'ENCOMENDA','APROVADO',909002),
(909016,'ENCOMENDA','APROVADO',909006),
(909017,'ENCOMENDA','APROVADO',909008);

INSERT INTO veiculo(id,modelo,ano,cor,placa,capacidade_passageiros,capacidade_carga_kg,marca,modalidade,ativo,id_prestador,descricao) VALUES
(909020,'Ducato Cargo',2024,'Branca','RF9A020',0,1200,'Fiat','ENCOMENDA',true,909002,'Carga compatível'),
(909021,'Spin',2023,'Prata','RF9A021',7,0,'Chevrolet','PESSOA',true,909002,'Somente pessoas'),
(909022,'Master inativa',2022,'Branca','RF9A022',0,1500,'Renault','ENCOMENDA',false,909002,'Veículo inativo'),
(909023,'Fiorino leve',2021,'Branca','RF9A023',0,100,'Fiat','ENCOMENDA',true,909002,'Carga insuficiente'),
(909024,'Spin quatro lugares',2024,'Cinza','RF9A024',4,0,'Chevrolet','PESSOA',true,909002,'Pessoas compatível'),
(909025,'Veículo externo',2024,'Preta','RF9A025',0,2000,'Mercedes','ENCOMENDA',true,909006,'Pertence a outro prestador'),
(909026,'Veículo autônomo',2024,'Branca','RF9A026',0,1000,'Fiat','ENCOMENDA',true,909008,'Autônomo sem motorista associado');

INSERT INTO endereco(id,cep,logradouro,numero,bairro,cidade,estado) VALUES
(909030,'60000-030','Rua Origem RF009','30','Centro','Fortaleza','CE'),
(909031,'11000-031','Rua Destino RF009','31','Centro','Santos','SP');

INSERT INTO solicitacao_frete(id,modalidade,data_hora_pretendida,status,id_passageiro,endereco_origem_id,endereco_destino_id,valor_cliente,condicoes_cliente) VALUES
(909040,'ENCOMENDA','2026-11-20 08:00','CONTRATADA',909001,909030,909031,480.00,'Entrega até 12:00'),
(909041,'PESSOA','2026-11-21 08:00','CONTRATADA',909001,909030,909031,740.00,'Três passageiros'),
(909042,'ENCOMENDA','2026-11-22 08:00','CONTRATADA',909001,909030,909031,510.00,'Frete interrompido'),
(909043,'ENCOMENDA','2026-11-23 08:00','CONTRATADA',909001,909030,909031,520.00,'Frete em execução'),
(909044,'ENCOMENDA','2026-11-24 08:00','CONTRATADA',909001,909030,909031,530.00,'Frete cancelado'),
(909045,'ENCOMENDA','2026-11-25 08:00','CONTRATADA',909001,909030,909031,540.00,'Frete de outro prestador'),
(909046,'ENCOMENDA','2026-11-26 08:00','CONTRATADA',909001,909030,909031,550.00,'Frete de autônomo sem motorista');

INSERT INTO solicitacao_encomenda(id,descricao_volumes,peso_estimado_kg,altura_cm,largura_cm,profundidade_cm,nome_contato_coleta,telefone_contato_coleta,nome_contato_entrega,telefone_contato_entrega) VALUES
(909040,'Doze caixas',180,60,50,40,'Coleta RF009','85909090400','Entrega RF009','85909090401'),
(909042,'Oito caixas',100,50,40,30,'Coleta RF009','85909090420','Entrega RF009','85909090421'),
(909043,'Oito caixas',100,50,40,30,'Coleta RF009','85909090430','Entrega RF009','85909090431'),
(909044,'Oito caixas',100,50,40,30,'Coleta RF009','85909090440','Entrega RF009','85909090441'),
(909045,'Oito caixas',100,50,40,30,'Coleta RF009','85909090450','Entrega RF009','85909090451'),
(909046,'Quatro caixas',80,40,30,20,'Coleta RF009','85909090460','Entrega RF009','85909090461');
INSERT INTO solicitacao_pessoa(id,quantidade_pessoas,possui_bagagem,descricao_bagagens)
VALUES (909041,3,true,'Três malas pequenas');

INSERT INTO proposta(id,status,valor,condicoes,id_solicitacao,id_prestador) VALUES
(909050,'ACEITA',480.00,'Entrega até 12:00',909040,909002),
(909051,'ACEITA',740.00,'Três passageiros',909041,909002),
(909052,'ACEITA',510.00,'Frete interrompido',909042,909002),
(909053,'ACEITA',520.00,'Frete em execução',909043,909002),
(909054,'ACEITA',530.00,'Frete cancelado',909044,909002),
(909055,'ACEITA',540.00,'Frete de outro prestador',909045,909006),
(909056,'ACEITA',550.00,'Frete de autônomo sem motorista',909046,909008);

INSERT INTO frete(id,valor_atual,condicoes_atuais,status,solicitacao_id,proposta_aceita_id,prestador_responsavel_id,motorista_atual_id,veiculo_atual_id,revisao_designacao) VALUES
(909060,480.00,'Entrega até 12:00','AGUARDANDO_DESIGNACAO',909040,909050,909002,NULL,NULL,0),
(909061,740.00,'Três passageiros','PRONTO_INICIO',909041,909051,909002,909010,909024,1),
(909062,510.00,'Frete interrompido','INTERROMPIDO',909042,909052,909002,909010,909020,2),
(909063,520.00,'Frete em execução','EM_EXECUCAO',909043,909053,909002,909010,909020,2),
(909064,530.00,'Frete cancelado','CANCELADO',909044,909054,909002,NULL,NULL,0),
(909065,540.00,'Frete de outro prestador','AGUARDANDO_DESIGNACAO',909045,909055,909006,NULL,NULL,0),
(909066,550.00,'Frete de autônomo sem motorista','AGUARDANDO_DESIGNACAO',909046,909056,909008,NULL,NULL,0);

INSERT INTO historico_execucao_frete(id,frete_id,usuario_autor_id,tipo_evento,justificativa,motorista_id,veiculo_id,data_hora) VALUES
(909070,909061,909002,'DESIGNACAO',NULL,909010,909024,'2026-11-20 09:00'),
(909071,909062,909002,'DESIGNACAO',NULL,909010,909020,'2026-11-20 09:00'),
(909072,909062,909002,'INICIO',NULL,909010,909020,'2026-11-20 10:00'),
(909073,909062,909002,'INTERRUPCAO','Pane controlada',909010,909020,'2026-11-20 10:30'),
(909074,909063,909002,'DESIGNACAO',NULL,909010,909020,'2026-11-20 09:00'),
(909075,909063,909002,'INICIO',NULL,909010,909020,'2026-11-20 10:00');
