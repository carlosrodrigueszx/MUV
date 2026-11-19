-- Dados adicionais do cadastro com conta. Motoristas legados continuam válidos.
ALTER TABLE motorista ADD COLUMN cpf_indice VARCHAR(64) UNIQUE;
ALTER TABLE motorista ADD COLUMN dados_cifrados TEXT;
ALTER TABLE motorista ADD COLUMN foto BYTEA;
ALTER TABLE motorista ADD COLUMN foto_tipo VARCHAR(32);
ALTER TABLE motorista ADD CONSTRAINT motorista_dados_conta_ck CHECK (
    (cpf_indice IS NULL AND dados_cifrados IS NULL)
    OR (cpf_indice IS NOT NULL AND dados_cifrados IS NOT NULL AND usuario_id IS NOT NULL)
);
