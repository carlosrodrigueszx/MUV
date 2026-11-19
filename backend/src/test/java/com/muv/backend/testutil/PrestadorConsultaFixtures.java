package com.muv.backend.testutil;

import org.springframework.jdbc.core.JdbcTemplate;

/** Registros determinísticos, sintéticos e exclusivos dos testes do RF005. */
public final class PrestadorConsultaFixtures {
    private PrestadorConsultaFixtures() {}

    public static void seed(JdbcTemplate jdbc) {
        clean(jdbc);
        jdbc.update("""
                INSERT INTO usuario (id, nome, telefone, email, senha) VALUES
                    (991001, 'Autônomo Horizonte Teste', '85990001001', 'autonomo-horizonte@example.test', 'teste'),
                    (991002, 'Empresa Pendente Teste', '85990001002', 'empresa-pendente@example.test', 'teste'),
                    (991003, 'Cooperativa Rota Azul Fictícia', '85990001003', 'cooperativa-rota-azul@example.test', 'teste'),
                    (991004, 'Autônomo Modalidade Incompatível', '85990001004', 'autonomo-incompativel@example.test', 'teste')
                """);
        jdbc.update("INSERT INTO prestador (id) VALUES (991001), (991002), (991003), (991004)");
        jdbc.update("""
                INSERT INTO prestador_autonomo (id, cpf) VALUES
                    (991001, 'AUT-TESTE-001'),
                    (991004, 'AUT-TESTE-004')
                """);
        jdbc.update("""
                INSERT INTO prestador_empresa (id, cnpj, razao_social, nome_fantasia) VALUES
                    (991002, 'CNPJ-TESTE-002', 'Empresa Pendente Fictícia LTDA', 'Empresa Pendente Teste'),
                    (991003, 'CNPJ-TESTE-003', 'Cooperativa Rota Azul Fictícia LTDA', 'Cooperativa Rota Azul Fictícia')
                """);
        jdbc.update("""
                INSERT INTO autorizacao_modalidade (id, modalidade, status, id_prestador) VALUES
                    (991001, 'PESSOA', 'APROVADO', 991001),
                    (991002, 'PESSOA', 'PENDENTE', 991002),
                    (991003, 'ENCOMENDA', 'APROVADO', 991003),
                    (991004, 'PESSOA', 'APROVADO', 991004)
                """);
        jdbc.update("""
                INSERT INTO regiao_atuacao (id, cidade, estado, id_prestador) VALUES
                    (991001, 'Fortaleza', 'CE', 991001),
                    (991002, 'Fortaleza', 'CE', 991002),
                    (991003, 'Recife', 'PE', 991003),
                    (991004, 'Fortaleza', 'CE', 991004)
                """);
        jdbc.update("""
                INSERT INTO veiculo (id, modelo, ano, cor, placa, capacidade_passageiros,
                    capacidade_carga_kg, marca, modalidade, ativo, id_prestador) VALUES
                    (991001, 'Micro-ônibus Ativo Teste', 2024, 'Branco', 'TST-1101', 20, 0, 'MUV', 'PESSOA', TRUE, 991001),
                    (991011, 'Van Inativa Teste', 2022, 'Prata', 'TST-1111', 12, 0, 'MUV', 'PESSOA', FALSE, 991001),
                    (991002, 'Van Empresa Pendente Teste', 2023, 'Azul', 'TST-1102', 12, 0, 'MUV', 'PESSOA', TRUE, 991002),
                    (991003, 'Furgão Cooperativa Teste', 2025, 'Branco', 'TST-1103', 0, 850, 'MUV', 'ENCOMENDA', TRUE, 991003),
                    (991004, 'Veículo Modalidade Incompatível', 2023, 'Cinza', 'TST-1104', 0, 500, 'MUV', 'ENCOMENDA', TRUE, 991004)
                """);
    }

    public static void clean(JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM veiculo WHERE id IN (991001, 991002, 991003, 991004, 991011)");
        jdbc.update("DELETE FROM autorizacao_modalidade WHERE id IN (991001, 991002, 991003, 991004)");
        jdbc.update("DELETE FROM regiao_atuacao WHERE id IN (991001, 991002, 991003, 991004)");
        jdbc.update("DELETE FROM prestador_empresa WHERE id IN (991002, 991003)");
        jdbc.update("DELETE FROM prestador_autonomo WHERE id IN (991001, 991004)");
        jdbc.update("DELETE FROM prestador WHERE id IN (991001, 991002, 991003, 991004)");
        jdbc.update("DELETE FROM usuario WHERE id IN (991001, 991002, 991003, 991004)");
    }
}
