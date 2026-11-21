INSERT INTO usuario(id,nome,telefone,email,senha) VALUES
(907001,'Cliente RF007','123','cliente907@example.test','fixture'),
(907002,'Prestador RF007','124','autor907@example.test','fixture'),
(907003,'Empresa RF007','125','empresa907@example.test','fixture');
INSERT INTO passageiro(id,cpf) VALUES (907001,'907.000.001-01');
INSERT INTO prestador(id) VALUES (907002),(907003);
INSERT INTO prestador_autonomo(id,cpf) VALUES (907002,'907.000.002-02');
INSERT INTO prestador_empresa(id,cnpj,razao_social,nome_fantasia) VALUES (907003,'90700000000133','Empresa RF007','Empresa RF007');
INSERT INTO autorizacao_modalidade(id,modalidade,status,id_prestador) VALUES
(907010,'PESSOA','APROVADO',907002),(907011,'ENCOMENDA','APROVADO',907002),(907012,'PESSOA','APROVADO',907003);
INSERT INTO endereco(id,cep,logradouro,numero,bairro,cidade,estado) VALUES
(907020,'60000-000','Rua do Sol','10','Centro','Fortaleza','CE'),(907021,'63000-000','Rua das Flores','34','Centro','Crato','CE');
INSERT INTO solicitacao_frete(id,modalidade,data_hora_pretendida,status,id_passageiro,id_prestador,endereco_origem_id,endereco_destino_id,valor_cliente,condicoes_cliente) VALUES
(907031,'PESSOA',CURRENT_TIMESTAMP,'ABERTA',907001,NULL,907020,907021,315.78,'Inclui pedágios'),
(907032,'ENCOMENDA',CURRENT_TIMESTAMP,'ABERTA',907001,907002,907021,907020,198.46,'Inclui coleta');
UPDATE solicitacao_frete SET percurso_km=142.35, deslocamento_minutos=125, duracao_minutos=240 WHERE id=907031;
INSERT INTO solicitacao_pessoa(id,quantidade_pessoas) VALUES (907031,3);
INSERT INTO solicitacao_encomenda(id,descricao_volumes,peso_estimado_kg,altura_cm,largura_cm,profundidade_cm,nome_contato_coleta,telefone_contato_coleta,nome_contato_entrega,telefone_contato_entrega)
VALUES (907032,'Duas caixas',5,20,20,20,'Origem','123','Destino','124');
