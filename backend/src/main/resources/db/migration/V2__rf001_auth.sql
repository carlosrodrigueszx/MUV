CREATE SEQUENCE usuario_id_seq;
SELECT setval('usuario_id_seq', COALESCE((SELECT MAX(id) FROM usuario), 0) + 1, false);
ALTER TABLE usuario ALTER COLUMN id SET DEFAULT nextval('usuario_id_seq');
CREATE UNIQUE INDEX usuario_email_normalizado_uq ON usuario (lower(trim(email)));

ALTER TABLE passageiro ALTER COLUMN cpf DROP NOT NULL;
ALTER TABLE passageiro ADD COLUMN dados_cifrados TEXT;
ALTER TABLE passageiro ADD COLUMN cpf_indice VARCHAR(64) UNIQUE;
ALTER TABLE passageiro ADD CONSTRAINT passageiro_identificacao_ck CHECK (
    (cpf IS NOT NULL AND cpf_indice IS NULL AND dados_cifrados IS NULL)
    OR (cpf IS NULL AND cpf_indice IS NOT NULL AND dados_cifrados IS NOT NULL)
);

CREATE SEQUENCE token_recuperacao_id_seq;
SELECT setval('token_recuperacao_id_seq', COALESCE((SELECT MAX(id) FROM token_recuperacao), 0) + 1, false);
ALTER TABLE token_recuperacao ALTER COLUMN id SET DEFAULT nextval('token_recuperacao_id_seq');
CREATE INDEX token_recuperacao_usuario_idx ON token_recuperacao (id_usuario);
