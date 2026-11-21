package com.muv.backend.service;

import com.muv.backend.dto.RespostaPrestador;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql("/db/rf007-solicitacoes.sql")
@Sql(scripts="/db/rf007-clean.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class PropostaIntegrationTest {
    @Autowired PropostaService service;
    @Autowired JdbcTemplate jdbc;

    RespostaPrestador manter(long id) {
        var d=service.avaliar(id,907002);
        return new RespostaPrestador(RespostaPrestador.Tipo.MANTER,d.valorCliente(),d.condicoesCliente(),null,null,UUID.randomUUID());
    }

    @Test void modalidadesDestinatariosERepeticao() {
        var pessoa=manter(907031);
        var encomenda=manter(907032);
        assertNotEquals(pessoa.valorRevisado(),encomenda.valorRevisado());
        long primeiro=service.responder(907031,907002,pessoa);
        assertEquals(primeiro,service.responder(907031,907002,pessoa));
        service.responder(907031,907003,pessoa);
        service.responder(907032,907002,encomenda);
        assertThrows(ResponseStatusException.class,()->service.responder(907032,907003,encomenda));
        jdbc.update("UPDATE solicitacao_frete SET id_prestador=NULL WHERE id=907032");
        assertThrows(ResponseStatusException.class,()->service.responder(907032,907003,encomenda));
        jdbc.update("UPDATE autorizacao_modalidade SET status='APROVADO', modalidade='ENCOMENDA' WHERE id=907012");
        jdbc.update("UPDATE solicitacao_frete SET id_prestador=907002 WHERE id=907032");
        assertThrows(ResponseStatusException.class,()->service.responder(907032,907003,encomenda));
        assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE usuario_destino_id=907001",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM proposta WHERE id_solicitacao=907031",Integer.class));
    }

    @Test void desfazPropostaSeNotificacaoFalhar() {
        var resposta=manter(907031);
        jdbc.execute("ALTER TABLE notificacao ADD CONSTRAINT rf007_falha_teste CHECK (solicitacao_id IS DISTINCT FROM 907031)");
        try {
            assertThrows(org.springframework.dao.DataAccessException.class,()->service.responder(907031,907002,resposta));
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM proposta WHERE id_solicitacao=907031",Integer.class));
        } finally { jdbc.execute("ALTER TABLE notificacao DROP CONSTRAINT rf007_falha_teste"); }
    }
}
