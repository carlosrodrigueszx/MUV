ALTER TABLE usuario ADD COLUMN termos_aceitos_em TIMESTAMP;
ALTER TABLE prestador ADD COLUMN conformidade_declarada_em TIMESTAMP;
ALTER TABLE prestador_autonomo ADD COLUMN habilitacao_cifrada TEXT;
ALTER TABLE prestador_autonomo ALTER COLUMN cpf DROP NOT NULL;
ALTER TABLE prestador_autonomo ADD CONSTRAINT prestador_autonomo_identificacao_ck
    CHECK (cpf IS NOT NULL OR habilitacao_cifrada IS NOT NULL);
