package com.muv.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = "selenium.browser=chrome")
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AchadoPerdidoMigrationTest {
    private static final String SCHEMA = "rf015_migration_" + UUID.randomUUID().toString().replace("-", "");
    private static final String UPGRADE_SCHEMA = SCHEMA + "_upgrade";

    @DynamicPropertySource
    static void isolatedSchema(DynamicPropertyRegistry properties) {
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA);
    }

    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;

    @Test
    void migracaoLimpaEUpgradePreservamItensEPermitemNovosIdsEImagens() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM achado_perdido", Integer.class)).isZero();
        assertUpgradePreservesLegacyRows();
    }

    private void assertUpgradePreservesLegacyRows() {
        Flyway.configure().dataSource(dataSource).schemas(UPGRADE_SCHEMA)
                .defaultSchema(UPGRADE_SCHEMA).target("9").load().migrate();
        JdbcTemplate legacy = new JdbcTemplate(dataSource);
        String prefix = UPGRADE_SCHEMA + ".";
        legacy.update("INSERT INTO " + prefix + "usuario (id,nome,telefone,email,senha) VALUES (981001,'Prestador legado','85999990000','rf015-legacy@example.test','hash')");
        legacy.update("INSERT INTO " + prefix + "prestador (id) VALUES (981001)");
        legacy.update("INSERT INTO " + prefix + "veiculo (id,modelo,ano,cor,placa,marca,modalidade,id_prestador) VALUES (981002,'Van',2024,'Branca','RF01501','MUV','PESSOA',981001)");
        legacy.update("INSERT INTO " + prefix + "achado_perdido (id,descricao,data_encontrado,status,id_veiculo,id_registrador) VALUES (981003,'Mochila legada',TIMESTAMP '2026-10-01 10:30:00','EM_CUSTODIA',981002,981001)");
        legacy.update("INSERT INTO " + prefix + "foto_item_perdido (id,url,id_item) VALUES (981004,'https://legacy.example/item.png',981003)");

        Flyway.configure().dataSource(dataSource).schemas(UPGRADE_SCHEMA)
                .defaultSchema(UPGRADE_SCHEMA).load().migrate();

        assertThat(legacy.queryForMap("SELECT descricao,status,id_frete FROM " + prefix + "achado_perdido WHERE id=981003"))
                .containsEntry("descricao", "Mochila legada")
                .containsEntry("status", "EM_CUSTODIA")
                .containsEntry("id_frete", null);
        assertThat(legacy.queryForObject("SELECT url FROM " + prefix + "foto_item_perdido WHERE id=981004", String.class))
                .isEqualTo("https://legacy.example/item.png");

        Number newItemId = legacy.queryForObject("""
                INSERT INTO %sachado_perdido (descricao,data_encontrado,status,id_veiculo,id_registrador)
                VALUES ('Mochila nova',CURRENT_TIMESTAMP,'EM_GUARDA',981002,981001) RETURNING id
                """.formatted(prefix), Number.class);
        assertThat(newItemId.longValue()).isGreaterThan(981003L);

        Number newPhotoId = legacy.queryForObject("""
                INSERT INTO %sfoto_item_perdido (url,conteudo,tipo,id_item)
                VALUES (NULL,decode('89504E470D0A1A0A','hex'),'image/png',%d) RETURNING id
                """.formatted(prefix, newItemId.longValue()), Number.class);
        assertThat(newPhotoId.longValue()).isGreaterThan(981004L);
        assertThat(legacy.queryForMap("SELECT url,octet_length(conteudo) AS bytes,tipo FROM " + prefix + "foto_item_perdido WHERE id=?", newPhotoId.longValue()))
                .containsEntry("url", null)
                .containsEntry("bytes", 8)
                .containsEntry("tipo", "image/png");
    }

    @AfterAll
    void dropTestSchemas() {
        jdbc.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
        jdbc.execute("DROP SCHEMA IF EXISTS " + UPGRADE_SCHEMA + " CASCADE");
    }
}
