package com.muv.backend.repository;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ActiveProfiles("test")
@Sql("/db/rf007-solicitacoes.sql")
@Sql(scripts="/db/rf007-clean.sql", executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class SolicitacoesDisponiveisRepositoryTest {
    @Autowired SolicitacoesDisponiveisRepository repositorio;
    @Autowired JdbcTemplate jdbc;

    @Test void mostraPublicasEDirecionadasAoPrestadorEAplicaFiltros() {
        jdbc.update("UPDATE solicitacao_frete SET status='CANCELADA' WHERE id=907031");
        assertEquals(List.of(907032L), repositorio.listar(907002, "TODAS", null, null)
                .stream().map(SolicitacoesDisponiveisRepository.Item::id).toList());
        jdbc.update("UPDATE solicitacao_frete SET status='ABERTA' WHERE id=907031");
        assertEquals(List.of(907031L), repositorio.listar(907003, "TODAS", null, null)
                .stream().map(SolicitacoesDisponiveisRepository.Item::id).toList());
        assertEquals(List.of(907032L), repositorio.listar(907002, "ENCOMENDA", "Crato", "CE")
                .stream().map(SolicitacoesDisponiveisRepository.Item::id).toList());
        assertTrue(repositorio.listar(907002, "PESSOA", "Crato", "CE").isEmpty());
        assertTrue(repositorio.listar(907002, "TODAS", "Inexistente", null).isEmpty());
        jdbc.update("UPDATE solicitacao_frete SET data_hora_pretendida='2020-11-23 10:00' WHERE id=907032");
        assertEquals(List.of(907032L), repositorio.listar(907002, "TODAS", null, null, LocalDate.of(2020, 11, 23))
                .stream().map(SolicitacoesDisponiveisRepository.Item::id).toList());
        assertTrue(repositorio.listar(907002, "ENCOMENDA", null, null, LocalDate.of(2020, 11, 24)).isEmpty());
    }

    @Test void indicaAutorizacaoSemDivulgarDadosDoCliente() {
        jdbc.update("UPDATE autorizacao_modalidade SET status='PENDENTE' WHERE id=907011");
        var itens = repositorio.listar(907002, "TODAS", null, null);
        assertTrue(itens.stream().filter(item -> item.id()==907031).findFirst().orElseThrow().autorizada());
        assertFalse(itens.stream().filter(item -> item.id()==907032).findFirst().orElseThrow().autorizada());
        assertFalse(itens.toString().contains("cliente907@example.test"));
    }
}
