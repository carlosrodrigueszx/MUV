package com.muv.backend.repository;

import com.muv.backend.dto.DesignacaoFrete.Contexto;
import com.muv.backend.dto.DesignacaoFrete.ConsultaCliente;
import com.muv.backend.dto.DesignacaoFrete.DesignacaoAtual;
import com.muv.backend.dto.DesignacaoFrete.MotoristaRecurso;
import com.muv.backend.dto.DesignacaoFrete.VeiculoRecurso;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DesignacaoRepository {
    private final JdbcClient db;

    public DesignacaoRepository(JdbcClient db) {
        this.db = db;
    }

    public Optional<Contexto> buscarContexto(long freteId, long prestadorId) {
        return buscarFrete(freteId, prestadorId).map(frete -> new Contexto(
                frete.freteId(),
                frete.solicitacaoId(),
                frete.modalidade(),
                frete.status(),
                frete.dataPretendida(),
                frete.origem(),
                frete.destino(),
                frete.valor(),
                frete.condicoes(),
                frete.revisao(),
                frete.quantidadePessoas(),
                frete.pesoEstimadoKg(),
                frete.prestadorAutonomo(),
                frete.designacaoAtual(),
                buscarMotoristasVerificados(prestadorId),
                buscarVeiculos(prestadorId)));
    }

    public Optional<ConsultaCliente> buscarDesignacaoCliente(long freteId, long clienteId) {
        return db.sql("""
                SELECT f.id, f.status, m.nome AS motorista_nome,
                       v.marca AS veiculo_marca, v.modelo AS veiculo_modelo,
                       v.placa AS veiculo_placa
                FROM frete f
                JOIN solicitacao_frete s ON s.id = f.solicitacao_id
                LEFT JOIN motorista m ON m.id = f.motorista_atual_id
                LEFT JOIN veiculo v ON v.id = f.veiculo_atual_id
                WHERE f.id = :freteId AND s.id_passageiro = :clienteId
                """)
                .param("freteId", freteId)
                .param("clienteId", clienteId)
                .query((rs, row) -> new ConsultaCliente(
                        rs.getLong("id"),
                        rs.getString("status"),
                        rs.getString("motorista_nome"),
                        rs.getString("veiculo_marca"),
                        rs.getString("veiculo_modelo"),
                        rs.getString("veiculo_placa")))
                .optional();
    }

    public Optional<FreteBloqueado> bloquearFrete(long freteId, long prestadorId) {
        return db.sql("""
                SELECT f.id, f.status, f.revisao_designacao, f.motorista_atual_id,
                       f.veiculo_atual_id, f.valor_atual, f.condicoes_atuais,
                       s.modalidade, s.id_passageiro,
                       sp.quantidade_pessoas, se.peso_estimado_kg,
                       (pa.id IS NOT NULL) AS prestador_autonomo
                FROM frete f
                JOIN solicitacao_frete s ON s.id = f.solicitacao_id
                LEFT JOIN solicitacao_pessoa sp ON sp.id = s.id
                LEFT JOIN solicitacao_encomenda se ON se.id = s.id
                LEFT JOIN prestador_autonomo pa ON pa.id = f.prestador_responsavel_id
                WHERE f.id = :freteId AND f.prestador_responsavel_id = :prestadorId
                FOR UPDATE OF f
                """)
                .param("freteId", freteId)
                .param("prestadorId", prestadorId)
                .query((rs, row) -> new FreteBloqueado(
                        rs.getLong("id"),
                        rs.getString("status"),
                        rs.getLong("revisao_designacao"),
                        rs.getString("modalidade"),
                        nullableInteger(rs, "quantidade_pessoas"),
                        rs.getBigDecimal("peso_estimado_kg"),
                        rs.getBoolean("prestador_autonomo"),
                        nullableLong(rs, "motorista_atual_id"),
                        nullableLong(rs, "veiculo_atual_id"),
                        rs.getLong("id_passageiro"),
                        rs.getBigDecimal("valor_atual"),
                        rs.getString("condicoes_atuais")))
                .optional();
    }

    public boolean motoristaVerificado(long prestadorId, long motoristaId) {
        return db.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM vinculo_motorista
                    WHERE prestador_id = :prestadorId
                      AND motorista_id = :motoristaId
                      AND status_verificacao = 'VERIFICADO'
                )
                """)
                .param("prestadorId", prestadorId)
                .param("motoristaId", motoristaId)
                .query(Boolean.class)
                .single();
    }

    public Optional<VeiculoRecurso> buscarVeiculoParaValidacao(long veiculoId) {
        return db.sql("""
                SELECT id, id_prestador, marca, modelo, ano, placa, cor, modalidade, ativo,
                       capacidade_passageiros, capacidade_carga_kg
                FROM veiculo
                WHERE id = :veiculoId
                """)
                .param("veiculoId", veiculoId)
                .query((rs, row) -> new VeiculoRecurso(
                        rs.getLong("id"),
                        rs.getLong("id_prestador"),
                        rs.getString("marca"),
                        rs.getString("modelo"),
                        rs.getInt("ano"),
                        rs.getString("placa"),
                        rs.getString("cor"),
                        rs.getString("modalidade"),
                        rs.getBoolean("ativo"),
                        rs.getInt("capacidade_passageiros"),
                        rs.getBigDecimal("capacidade_carga_kg")))
                .optional();
    }

    public long registrarDesignacao(
            long freteId,
            long prestadorId,
            long motoristaId,
            long veiculoId,
            long revisaoEsperada) {
        int atualizados = db.sql("""
                UPDATE frete
                SET motorista_atual_id = :motoristaId,
                    veiculo_atual_id = :veiculoId,
                    status = 'PRONTO_INICIO',
                    revisao_designacao = revisao_designacao + 1
                WHERE id = :freteId
                  AND prestador_responsavel_id = :prestadorId
                  AND status = 'AGUARDANDO_DESIGNACAO'
                  AND revisao_designacao = :revisao
                """)
                .param("motoristaId", motoristaId)
                .param("veiculoId", veiculoId)
                .param("freteId", freteId)
                .param("prestadorId", prestadorId)
                .param("revisao", revisaoEsperada)
                .update();
        if (atualizados != 1) {
            return -1;
        }
        db.sql("""
                INSERT INTO historico_execucao_frete
                    (frete_id, usuario_autor_id, tipo_evento, motorista_id, veiculo_id)
                VALUES (:freteId, :autorId, 'DESIGNACAO', :motoristaId, :veiculoId)
                """)
                .param("freteId", freteId)
                .param("autorId", prestadorId)
                .param("motoristaId", motoristaId)
                .param("veiculoId", veiculoId)
                .update();
        return revisaoEsperada + 1;
    }

    public long registrarSubstituicao(
            long freteId,
            long prestadorId,
            long clienteId,
            long motoristaId,
            long veiculoId,
            long revisaoEsperada,
            String justificativa) {
        int atualizados = db.sql("""
                UPDATE frete
                SET motorista_atual_id = :motoristaId,
                    veiculo_atual_id = :veiculoId,
                    revisao_designacao = revisao_designacao + 1
                WHERE id = :freteId
                  AND prestador_responsavel_id = :prestadorId
                  AND status IN ('PRONTO_INICIO', 'INTERROMPIDO')
                  AND revisao_designacao = :revisao
                """)
                .param("motoristaId", motoristaId)
                .param("veiculoId", veiculoId)
                .param("freteId", freteId)
                .param("prestadorId", prestadorId)
                .param("revisao", revisaoEsperada)
                .update();
        if (atualizados != 1) {
            return -1;
        }
        db.sql("""
                INSERT INTO historico_execucao_frete
                    (frete_id, usuario_autor_id, tipo_evento, justificativa,
                     motorista_id, veiculo_id)
                VALUES (:freteId, :autorId, 'SUBSTITUICAO_SEM_ALTERACAO', :justificativa,
                        :motoristaId, :veiculoId)
                """)
                .param("freteId", freteId)
                .param("autorId", prestadorId)
                .param("justificativa", justificativa)
                .param("motoristaId", motoristaId)
                .param("veiculoId", veiculoId)
                .update();
        db.sql("""
                INSERT INTO notificacao (titulo, mensagem, usuario_destino_id, frete_id)
                VALUES ('Recursos do frete atualizados',
                        'O prestador substituiu o motorista ou o veículo do seu frete.',
                        :clienteId, :freteId)
                """)
                .param("clienteId", clienteId)
                .param("freteId", freteId)
                .update();
        return revisaoEsperada + 1;
    }

    public long registrarSolicitacaoAcordo(
            long freteId,
            long prestadorId,
            long clienteId,
            long motoristaId,
            long veiculoId,
            long revisaoOrigem,
            BigDecimal novoValor,
            String novasCondicoes,
            String justificativa) {
        long id = db.sql("""
                INSERT INTO solicitacao_alteracao_acordo
                    (novo_valor, novas_condicoes, justificativa, id_frete,
                     prestador_solicitante_id, novo_motorista_id, novo_veiculo_id,
                     revisao_origem)
                VALUES (:novoValor, :novasCondicoes, :justificativa, :freteId,
                        :prestadorId, :motoristaId, :veiculoId, :revisaoOrigem)
                RETURNING id
                """)
                .param("novoValor", novoValor)
                .param("novasCondicoes", novasCondicoes)
                .param("justificativa", justificativa)
                .param("freteId", freteId)
                .param("prestadorId", prestadorId)
                .param("motoristaId", motoristaId)
                .param("veiculoId", veiculoId)
                .param("revisaoOrigem", revisaoOrigem)
                .query(Long.class)
                .single();
        db.sql("""
                INSERT INTO notificacao (titulo, mensagem, usuario_destino_id, frete_id)
                VALUES ('Nova proposta de alteração do frete',
                        'O prestador solicitou sua aprovação para alterar o frete.',
                        :clienteId, :freteId)
                """)
                .param("clienteId", clienteId)
                .param("freteId", freteId)
                .update();
        return id;
    }

    public Optional<AcordoBloqueado> bloquearAcordo(
            long freteId,
            long acordoId,
            long clienteId) {
        return db.sql("""
                SELECT a.id, a.status AS acordo_status, a.novo_valor, a.novas_condicoes,
                       a.justificativa, a.novo_motorista_id, a.novo_veiculo_id,
                       a.revisao_origem, f.status AS frete_status, f.revisao_designacao,
                       f.prestador_responsavel_id, s.modalidade, s.id_passageiro,
                       sp.quantidade_pessoas, se.peso_estimado_kg
                FROM solicitacao_alteracao_acordo a
                JOIN frete f ON f.id = a.id_frete
                JOIN solicitacao_frete s ON s.id = f.solicitacao_id
                LEFT JOIN solicitacao_pessoa sp ON sp.id = s.id
                LEFT JOIN solicitacao_encomenda se ON se.id = s.id
                WHERE a.id = :acordoId AND f.id = :freteId AND s.id_passageiro = :clienteId
                FOR UPDATE OF a, f
                """)
                .param("acordoId", acordoId)
                .param("freteId", freteId)
                .param("clienteId", clienteId)
                .query((rs, row) -> new AcordoBloqueado(
                        rs.getLong("id"),
                        rs.getString("acordo_status"),
                        rs.getBigDecimal("novo_valor"),
                        rs.getString("novas_condicoes"),
                        rs.getString("justificativa"),
                        rs.getLong("novo_motorista_id"),
                        rs.getLong("novo_veiculo_id"),
                        rs.getLong("revisao_origem"),
                        rs.getString("frete_status"),
                        rs.getLong("revisao_designacao"),
                        rs.getLong("prestador_responsavel_id"),
                        rs.getString("modalidade"),
                        nullableInteger(rs, "quantidade_pessoas"),
                        rs.getBigDecimal("peso_estimado_kg")))
                .optional();
    }

    public void rejeitarAcordo(long acordoId, long freteId, long prestadorId) {
        db.sql("""
                UPDATE solicitacao_alteracao_acordo
                SET status = 'REJEITADA', data_hora_resposta = CURRENT_TIMESTAMP
                WHERE id = :acordoId AND id_frete = :freteId AND status = 'PENDENTE'
                """)
                .param("acordoId", acordoId)
                .param("freteId", freteId)
                .update();
        notificarRespostaAcordo(prestadorId, freteId, false);
    }

    public long aplicarAcordo(long freteId, AcordoBloqueado acordo) {
        int acordoAtualizado = db.sql("""
                UPDATE solicitacao_alteracao_acordo
                SET status = 'ACEITA', data_hora_resposta = CURRENT_TIMESTAMP
                WHERE id = :acordoId AND id_frete = :freteId AND status = 'PENDENTE'
                """)
                .param("acordoId", acordo.id())
                .param("freteId", freteId)
                .update();
        int freteAtualizado = db.sql("""
                UPDATE frete
                SET valor_atual = :valor,
                    condicoes_atuais = :condicoes,
                    motorista_atual_id = :motoristaId,
                    veiculo_atual_id = :veiculoId,
                    revisao_designacao = revisao_designacao + 1
                WHERE id = :freteId AND prestador_responsavel_id = :prestadorId
                  AND revisao_designacao = :revisao
                  AND status IN ('PRONTO_INICIO', 'INTERROMPIDO')
                """)
                .param("valor", acordo.novoValor())
                .param("condicoes", acordo.novasCondicoes())
                .param("motoristaId", acordo.novoMotoristaId())
                .param("veiculoId", acordo.novoVeiculoId())
                .param("freteId", freteId)
                .param("prestadorId", acordo.prestadorId())
                .param("revisao", acordo.revisaoOrigem())
                .update();
        if (acordoAtualizado != 1 || freteAtualizado != 1) {
            return -1;
        }
        db.sql("""
                INSERT INTO historico_execucao_frete
                    (frete_id, usuario_autor_id, tipo_evento, justificativa,
                     motorista_id, veiculo_id, solicitacao_alteracao_id)
                VALUES (:freteId, :autorId, 'SUBSTITUICAO_COM_ACORDO', :justificativa,
                        :motoristaId, :veiculoId, :acordoId)
                """)
                .param("freteId", freteId)
                .param("autorId", acordo.prestadorId())
                .param("justificativa", acordo.justificativa())
                .param("motoristaId", acordo.novoMotoristaId())
                .param("veiculoId", acordo.novoVeiculoId())
                .param("acordoId", acordo.id())
                .update();
        notificarRespostaAcordo(acordo.prestadorId(), freteId, true);
        return acordo.revisaoOrigem() + 1;
    }

    private void notificarRespostaAcordo(long prestadorId, long freteId, boolean aceita) {
        db.sql("""
                INSERT INTO notificacao (titulo, mensagem, usuario_destino_id, frete_id)
                VALUES (:titulo, :mensagem, :prestadorId, :freteId)
                """)
                .param("titulo", aceita ? "Alteração aceita" : "Alteração rejeitada")
                .param("mensagem", aceita
                        ? "O cliente aceitou a alteração proposta para o frete."
                        : "O cliente rejeitou a alteração proposta para o frete.")
                .param("prestadorId", prestadorId)
                .param("freteId", freteId)
                .update();
    }

    private Optional<FreteRow> buscarFrete(long freteId, long prestadorId) {
        return db.sql("""
                SELECT f.id AS frete_id, f.status, f.valor_atual, f.condicoes_atuais,
                       f.revisao_designacao, s.id AS solicitacao_id, s.modalidade,
                       s.data_hora_pretendida,
                       concat_ws(' - ', origem.cidade, origem.estado) AS origem,
                       concat_ws(' - ', destino.cidade, destino.estado) AS destino,
                       sp.quantidade_pessoas, se.peso_estimado_kg,
                       (pa.id IS NOT NULL) AS prestador_autonomo,
                       m.id AS motorista_id, m.nome AS motorista_nome,
                       v.id AS veiculo_id, v.marca AS veiculo_marca,
                       v.modelo AS veiculo_modelo, v.placa AS veiculo_placa
                FROM frete f
                JOIN solicitacao_frete s ON s.id = f.solicitacao_id
                JOIN endereco origem ON origem.id = s.endereco_origem_id
                JOIN endereco destino ON destino.id = s.endereco_destino_id
                LEFT JOIN solicitacao_pessoa sp ON sp.id = s.id
                LEFT JOIN solicitacao_encomenda se ON se.id = s.id
                LEFT JOIN prestador_autonomo pa ON pa.id = f.prestador_responsavel_id
                LEFT JOIN motorista m ON m.id = f.motorista_atual_id
                LEFT JOIN veiculo v ON v.id = f.veiculo_atual_id
                WHERE f.id = :freteId AND f.prestador_responsavel_id = :prestadorId
                """)
                .param("freteId", freteId)
                .param("prestadorId", prestadorId)
                .query(DesignacaoRepository::mapFrete)
                .optional();
    }

    private List<MotoristaRecurso> buscarMotoristasVerificados(long prestadorId) {
        return db.sql("""
                SELECT m.id, m.nome
                FROM vinculo_motorista vm
                JOIN motorista m ON m.id = vm.motorista_id
                WHERE vm.prestador_id = :prestadorId
                  AND vm.status_verificacao = 'VERIFICADO'
                ORDER BY m.nome, m.id
                """)
                .param("prestadorId", prestadorId)
                .query((rs, row) -> new MotoristaRecurso(rs.getLong("id"), rs.getString("nome")))
                .list();
    }

    private List<VeiculoRecurso> buscarVeiculos(long prestadorId) {
        return db.sql("""
                SELECT id, id_prestador, marca, modelo, ano, placa, cor, modalidade, ativo,
                       capacidade_passageiros, capacidade_carga_kg
                FROM veiculo
                WHERE id_prestador = :prestadorId
                ORDER BY marca, modelo, id
                """)
                .param("prestadorId", prestadorId)
                .query((rs, row) -> new VeiculoRecurso(
                        rs.getLong("id"),
                        rs.getLong("id_prestador"),
                        rs.getString("marca"),
                        rs.getString("modelo"),
                        rs.getInt("ano"),
                        rs.getString("placa"),
                        rs.getString("cor"),
                        rs.getString("modalidade"),
                        rs.getBoolean("ativo"),
                        rs.getInt("capacidade_passageiros"),
                        rs.getBigDecimal("capacidade_carga_kg")))
                .list();
    }

    private static FreteRow mapFrete(ResultSet rs, int row) throws SQLException {
        Long motoristaId = nullableLong(rs, "motorista_id");
        Long veiculoId = nullableLong(rs, "veiculo_id");
        DesignacaoAtual designacao = motoristaId == null && veiculoId == null
                ? null
                : new DesignacaoAtual(
                        motoristaId,
                        rs.getString("motorista_nome"),
                        veiculoId,
                        rs.getString("veiculo_marca"),
                        rs.getString("veiculo_modelo"),
                        rs.getString("veiculo_placa"));
        return new FreteRow(
                rs.getLong("frete_id"),
                rs.getLong("solicitacao_id"),
                rs.getString("modalidade"),
                rs.getString("status"),
                rs.getObject("data_hora_pretendida", LocalDateTime.class),
                rs.getString("origem"),
                rs.getString("destino"),
                rs.getBigDecimal("valor_atual"),
                rs.getString("condicoes_atuais"),
                rs.getLong("revisao_designacao"),
                nullableInteger(rs, "quantidade_pessoas"),
                rs.getBigDecimal("peso_estimado_kg"),
                rs.getBoolean("prestador_autonomo"),
                designacao);
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static Integer nullableInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private record FreteRow(
            long freteId,
            long solicitacaoId,
            String modalidade,
            String status,
            LocalDateTime dataPretendida,
            String origem,
            String destino,
            BigDecimal valor,
            String condicoes,
            long revisao,
            Integer quantidadePessoas,
            BigDecimal pesoEstimadoKg,
            boolean prestadorAutonomo,
            DesignacaoAtual designacaoAtual) {}

    public record FreteBloqueado(
            long freteId,
            String status,
            long revisao,
            String modalidade,
            Integer quantidadePessoas,
            BigDecimal pesoEstimadoKg,
            boolean prestadorAutonomo,
            Long motoristaAtualId,
            Long veiculoAtualId,
            long clienteId,
            BigDecimal valorAtual,
            String condicoesAtuais) {}

    public record AcordoBloqueado(
            long id,
            String status,
            BigDecimal novoValor,
            String novasCondicoes,
            String justificativa,
            long novoMotoristaId,
            long novoVeiculoId,
            long revisaoOrigem,
            String freteStatus,
            long revisaoFrete,
            long prestadorId,
            String modalidade,
            Integer quantidadePessoas,
            BigDecimal pesoEstimadoKg) {}
}
