package com.muv.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "selenium.browser=chrome")
@ActiveProfiles("rf001-local")
@Transactional
@Sql(scripts = {"/db/rf009-clean.sql", "/db/rf009-designacao.sql"})
class DesignacaoRepositoryTest {

    @Autowired
    private DesignacaoRepository repository;

    @Test
    void carregaFreteRecursosVerificadosEDadosDeConcorrencia() {
        var contexto = repository.buscarContexto(909060L, 909002L).orElseThrow();

        assertThat(contexto.freteId()).isEqualTo(909060L);
        assertThat(contexto.solicitacaoId()).isEqualTo(909040L);
        assertThat(contexto.modalidade()).isEqualTo("ENCOMENDA");
        assertThat(contexto.pesoEstimadoKg()).isEqualByComparingTo("180");
        assertThat(contexto.quantidadePessoas()).isNull();
        assertThat(contexto.revisao()).isZero();
        assertThat(contexto.designacaoAtual()).isNull();
        assertThat(contexto.motoristas())
                .extracting(motorista -> motorista.id())
                .containsExactlyInAnyOrder(909010L, 909018L);
        assertThat(contexto.veiculos())
                .extracting(veiculo -> veiculo.id())
                .containsExactlyInAnyOrder(909020L, 909021L, 909022L, 909023L, 909024L);
    }

    @Test
    void nuncaExibeRecursosDeOutroPrestador() {
        var contexto = repository.buscarContexto(909060L, 909002L).orElseThrow();

        assertThat(contexto.motoristas())
                .noneMatch(motorista -> motorista.id() == 909013L);
        assertThat(contexto.veiculos())
                .noneMatch(veiculo -> veiculo.id() == 909025L || veiculo.id() == 909026L);
        assertThat(repository.buscarContexto(909060L, 909006L)).isEmpty();
    }

    @Test
    void carregaDesignacaoAtualSemExporDadosPessoaisDoMotorista() {
        var contexto = repository.buscarContexto(909061L, 909002L).orElseThrow();

        assertThat(contexto.revisao()).isEqualTo(1L);
        assertThat(contexto.quantidadePessoas()).isEqualTo(3);
        assertThat(contexto.designacaoAtual()).isNotNull();
        assertThat(contexto.designacaoAtual().motoristaId()).isEqualTo(909010L);
        assertThat(contexto.designacaoAtual().motoristaNome()).isEqualTo("Motorista Verificado");
        assertThat(contexto.designacaoAtual().veiculoPlaca()).isEqualTo("RF9A024");
    }
}
