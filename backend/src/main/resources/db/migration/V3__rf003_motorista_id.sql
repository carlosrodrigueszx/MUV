-- V1 não definia geração automática para o ID do motorista.
CREATE SEQUENCE motorista_id_seq;
SELECT setval('motorista_id_seq', COALESCE((SELECT MAX(id) FROM motorista), 0) + 1, false);
ALTER TABLE motorista ALTER COLUMN id SET DEFAULT nextval('motorista_id_seq');
