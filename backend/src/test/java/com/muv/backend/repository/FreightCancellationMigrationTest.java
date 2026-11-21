package com.muv.backend.repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;

class FreightCancellationMigrationTest {
    @Test
    void integratedMigrationsGenerateIdsAfterExistingHistoryAndNotifications() {
        String url = System.getenv("MUV_RF011_DB_URL");
        if (url == null) throw new IllegalStateException("Configure MUV_RF011_DB_URL para um PostgreSQL de teste.");
        String user = System.getenv().getOrDefault("MUV_RF011_DB_USER", "bia");
        String password = System.getenv().getOrDefault("MUV_RF011_DB_PASSWORD", "");
        String schema = "rf011_flyway_" + UUID.randomUUID().toString().replace("-", "");
        Flyway.configure().dataSource(url, user, password).schemas(schema).defaultSchema(schema)
                .target("6").load().migrate();
        var datasource = new DriverManagerDataSource(url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema, user, password);
        var db = JdbcClient.create(datasource);
        new ResourceDatabasePopulator(new ClassPathResource("db/rf011-fixture.sql")).execute(datasource);
        db.sql("INSERT INTO historico_execucao_frete (id,frete_id,usuario_autor_id,tipo_evento,descricao) VALUES (500,11010,11001,'OCORRENCIA','Registro prévio')").update();
        db.sql("INSERT INTO notificacao (id,titulo,mensagem,usuario_destino_id,frete_id) VALUES (700,'Anterior','Registro prévio',11001,11010)").update();
        var flyway = Flyway.configure().dataSource(url, user, password).schemas(schema).defaultSchema(schema)
                .load();
        flyway.migrate();
        flyway.validate();
        assertThat(Arrays.stream(flyway.info().applied())
                .filter(migration -> migration.getVersion() != null)
                .map(migration -> migration.getVersion().getVersion()))
                .contains("6", "7", "7.1", "8", "9");
        var repository = new FreightCancellationRepository(db);
        var cancelledAtUtc = LocalDateTime.of(2026, 11, 19, 17, 30);
        repository.addCancellationEvent(11010, 11001, "Teste de IDs integrados", cancelledAtUtc);
        repository.notifyCancellation(11010, 11002, cancelledAtUtc);
        assertThat(db.sql("SELECT id FROM historico_execucao_frete WHERE tipo_evento = 'CANCELAMENTO'")
                .query(Long.class).single()).isEqualTo(501L);
        assertThat(db.sql("SELECT id FROM notificacao WHERE titulo = 'Frete cancelado'")
                .query(Long.class).single()).isEqualTo(701L);
        assertThat(db.sql("SELECT count(*) FROM historico_execucao_frete WHERE id = 500").query(Long.class).single()).isEqualTo(1);
        assertThat(db.sql("SELECT count(*) FROM notificacao WHERE id = 700").query(Long.class).single()).isEqualTo(1);
    }
}
