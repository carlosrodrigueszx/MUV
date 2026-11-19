package com.muv.backend.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AuthRepository {
    private final JdbcClient db;

    public AuthRepository(JdbcClient db) {
        this.db = db;
    }

    public record Account(long id, String nome, String email, String senha) {}

    public Optional<Account> findAccount(String email) {
        return db.sql("SELECT id, nome, email, senha FROM usuario WHERE lower(trim(email)) = :email")
                .param("email", email)
                .query((rs, row) -> new Account(rs.getLong("id"), rs.getString("nome"),
                        rs.getString("email"), rs.getString("senha"))).optional();
    }

    public boolean hasCpf(String cpf, String index) {
        return db.sql("""
                SELECT EXISTS (SELECT 1 FROM passageiro
                    WHERE cpf_indice = :indice OR regexp_replace(cpf, '[^0-9]', '', 'g') = :cpf
                    UNION ALL SELECT 1 FROM prestador_autonomo
                    WHERE regexp_replace(cpf, '[^0-9]', '', 'g') = :cpf)
                """).param("indice", index).param("cpf", cpf).query(Boolean.class).single();
    }

    public long insertAccount(String nome, String telefone, String email, String passwordHash) {
        return db.sql("""
                INSERT INTO usuario (nome, telefone, email, senha, is_admin)
                VALUES (:nome, :telefone, :email, :senha, false) RETURNING id
                """).param("nome", nome).param("telefone", telefone).param("email", email)
                .param("senha", passwordHash).query(Long.class).single();
    }

    public void recordTermsAcceptance(long id) {
        db.sql("UPDATE usuario SET termos_aceitos_em = CURRENT_TIMESTAMP WHERE id = :id")
                .param("id", id).update();
    }

    public void insertPassenger(long id, String index, String encrypted) {
        db.sql("INSERT INTO passageiro (id, cpf_indice, dados_cifrados) VALUES (:id, :indice, :dados)")
                .param("id", id).param("indice", index).param("dados", encrypted).update();
    }

    public boolean hasCompanyDocument(String cnpj) {
        return db.sql("""
                SELECT EXISTS (SELECT 1 FROM prestador_empresa
                    WHERE regexp_replace(cnpj, '[^0-9]', '', 'g') = :cnpj)
                """).param("cnpj", cnpj).query(Boolean.class).single();
    }

    public void insertProvider(long id) {
        db.sql("INSERT INTO prestador (id, conformidade_declarada_em) VALUES (:id, CURRENT_TIMESTAMP)")
                .param("id", id).update();
    }

    public void insertCompany(long id, String cnpj, String legalName, String tradeName) {
        db.sql("""
                INSERT INTO prestador_empresa (id, cnpj, razao_social, nome_fantasia)
                VALUES (:id, :cnpj, :legalName, :tradeName)
                """).param("id", id).param("cnpj", cnpj).param("legalName", legalName)
                .param("tradeName", tradeName).update();
    }

    public void insertIndependent(long id, String encryptedLicense) {
        db.sql("""
                INSERT INTO prestador_autonomo (id, habilitacao_cifrada)
                VALUES (:id, :encryptedLicense)
                """).param("id", id).param("encryptedLicense", encryptedLicense).update();
    }

    public void lockAccount(long id) {
        db.sql("SELECT id FROM usuario WHERE id = :id FOR UPDATE")
                .param("id", id).query(Long.class).single();
    }

    public void insertReset(long id, String digest, LocalDateTime expiry) {
        db.sql("INSERT INTO token_recuperacao (token, expira_em, id_usuario) VALUES (:token, :expira, :id)")
                .param("token", digest).param("expira", expiry).param("id", id).update();
    }

    public Optional<Long> resetOwner(String digest) {
        return db.sql("SELECT id_usuario FROM token_recuperacao WHERE token = :token")
                .param("token", digest).query(Long.class).optional();
    }

    public boolean consumeReset(String digest, LocalDateTime now) {
        return db.sql("""
                UPDATE token_recuperacao SET usado_em = :now
                WHERE token = :token AND usado_em IS NULL AND expira_em > :now
                """).param("token", digest).param("now", now).update() == 1;
    }

    public void invalidateResets(long id, LocalDateTime now) {
        db.sql("UPDATE token_recuperacao SET usado_em = :now WHERE id_usuario = :id AND usado_em IS NULL")
                .param("id", id).param("now", now).update();
    }

    public void replacePassword(long id, String hash) {
        db.sql("UPDATE usuario SET senha = :hash WHERE id = :id")
                .param("id", id).param("hash", hash).update();
    }
}
