INSERT INTO usuario(id,nome,telefone,email,senha,is_admin) VALUES
(910001,'Cliente Teste','85910000001','cliente910@example.test','fixture',false),
(910002,'Empresa Teste','85910000002','empresa910@example.test','fixture',false),
(910003,'Motorista Atual','85910000003','atual910@example.test','fixture',false),
(910004,'Colega Verificado','85910000004','colega910@example.test','fixture',false),
(910005,'Outra Empresa','85910000005','outra910@example.test','fixture',false),
(910006,'Motorista Externo','85910000006','externo910@example.test','fixture',false),
(910007,'Autônomo Teste','85910000007','autonomo910@example.test','fixture',false);

INSERT INTO passageiro(id,cpf) VALUES (910001,'910.000.001-01');
INSERT INTO prestador(id) VALUES (910002),(910005),(910007);
INSERT INTO prestador_empresa(id,cnpj,razao_social,nome_fantasia) VALUES
(910002,'91000000000102','Empresa Teste Ltda','Empresa Teste'),
(910005,'91000000000105','Outra Empresa Ltda','Outra Empresa');
INSERT INTO prestador_autonomo(id,cpf) VALUES (910007,'910.000.007-07');

INSERT INTO motorista(id,nome,telefone,cnh,categoria_cnh,usuario_id) VALUES
(910011,'Motorista Atual','85910000003','91000000011','D',910003),
(910012,'Colega Verificado','85910000004','91000000012','D',910004),
(910013,'Motorista Externo','85910000006','91000000013','D',910006);
INSERT INTO vinculo_motorista(prestador_id,motorista_id,status_verificacao,verificado_por_id,data_verificacao) VALUES
(910002,910011,'VERIFICADO',910002,CURRENT_TIMESTAMP),
(910002,910012,'VERIFICADO',910002,CURRENT_TIMESTAMP),
(910005,910013,'VERIFICADO',910005,CURRENT_TIMESTAMP);

INSERT INTO veiculo(id,modelo,ano,cor,placa,capacidade_passageiros,capacidade_carga_kg,marca,modalidade,ativo,id_prestador) VALUES
(910021,'Minivan',2024,'Prata','RF1A021',7,0,'Marca Teste','PESSOA',true,910002),
(910022,'Furgão',2023,'Branca','RF1A022',0,700,'Marca Teste','ENCOMENDA',true,910002);
INSERT INTO endereco(id,cep,logradouro,numero,bairro,cidade,estado) VALUES
(910031,'60000-031','Rua Origem','31','Centro','Fortaleza','CE'),
(910032,'11000-032','Rua Destino','32','Centro','Santos','SP');

INSERT INTO solicitacao_frete(id,modalidade,data_hora_pretendida,status,id_passageiro,endereco_origem_id,endereco_destino_id,valor_cliente,condicoes_cliente) VALUES
(910041,'PESSOA','2026-11-23 08:00','CONTRATADA',910001,910031,910032,100,'Pessoas'),
(910042,'ENCOMENDA','2026-11-23 08:00','CONTRATADA',910001,910031,910032,100,'Encomenda'),
(910043,'ENCOMENDA','2026-11-23 08:00','CONTRATADA',910001,910031,910032,100,'Frete em execução'),
(910044,'PESSOA','2026-11-23 08:00','CONTRATADA',910001,910031,910032,100,'Frete do autônomo');
INSERT INTO solicitacao_pessoa(id,quantidade_pessoas,possui_bagagem) VALUES
(910041,4,false),(910044,2,false);
INSERT INTO solicitacao_encomenda(id,descricao_volumes,peso_estimado_kg,altura_cm,largura_cm,profundidade_cm,nome_contato_coleta,telefone_contato_coleta,nome_contato_entrega,telefone_contato_entrega) VALUES
(910042,'Caixa',25,50,40,30,'Coleta','85911111001','Entrega','85911111002'),
(910043,'Caixa',25,50,40,30,'Coleta','85911111001','Entrega','85911111002');

INSERT INTO proposta(id,status,valor,condicoes,id_solicitacao,id_prestador) VALUES
(910051,'ACEITA',100,'Pessoas',910041,910002),
(910052,'ACEITA',100,'Encomenda',910042,910002),
(910053,'ACEITA',100,'Encomenda',910043,910002),
(910054,'ACEITA',100,'Pessoas',910044,910007);

INSERT INTO frete(id,valor_atual,condicoes_atuais,status,solicitacao_id,proposta_aceita_id,prestador_responsavel_id,motorista_atual_id,veiculo_atual_id,revisao_designacao) VALUES
(910061,100,'Pessoas','PRONTO_INICIO',910041,910051,910002,910011,910021,1),
(910062,100,'Encomenda','INTERROMPIDO',910042,910052,910002,910011,910022,2),
(910063,100,'Encomenda','EM_EXECUCAO',910043,910053,910002,910011,910022,1),
(910064,100,'Pessoas','AGUARDANDO_DESIGNACAO',910044,910054,910007,NULL,NULL,0);
INSERT INTO historico_execucao_frete(id,frete_id,usuario_autor_id,tipo_evento,descricao,justificativa,motorista_id,veiculo_id,data_hora) VALUES
(910071,910062,910003,'INICIO',NULL,NULL,910011,910022,'2026-11-23 08:00'),
(910072,910062,910002,'INTERRUPCAO',NULL,'Via bloqueada',910011,910022,'2026-11-23 08:10'),
(910073,910063,910003,'INICIO',NULL,NULL,910011,910022,'2026-11-23 08:00'),
(910074,910063,910002,'OCORRENCIA','Trânsito lento',NULL,910011,910022,'2026-11-23 08:10');
