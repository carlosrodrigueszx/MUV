package com.muv.backend.repository;

public final class ModalidadesDemonstracao {
    private ModalidadesDemonstracao() {}

    // Aprovações efetivas para a consulta, sem alterar o estado persistido.
    private static final String CONSULTA = """
            WITH modalidades_efetivas AS (
                SELECT id_prestador, modalidade, status
                FROM autorizacao_modalidade
                WHERE status = 'APROVADO'
                UNION
                SELECT v.id_prestador, m.modalidade, 'APROVADO'
                FROM veiculo v
                JOIN (VALUES ('PESSOA'), ('ENCOMENDA')) m(modalidade)
                  ON v.modalidade IN (m.modalidade, 'AMBOS')
                WHERE v.ativo
                  AND NOT EXISTS (
                      SELECT 1 FROM autorizacao_modalidade a
                      WHERE a.id_prestador = v.id_prestador AND a.modalidade = m.modalidade
                        AND a.status NOT IN ('PENDENTE', 'APROVADO')
                  )
            )
            """;

    public static String aplicar(String consulta, boolean demonstracao) {
        return demonstracao
                ? CONSULTA + consulta.replace("autorizacao_modalidade", "modalidades_efetivas")
                : consulta;
    }
}
