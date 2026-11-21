CREATE SEQUENCE veiculo_id_seq;
SELECT setval('veiculo_id_seq', COALESCE((SELECT MAX(id) FROM veiculo), 0) + 1, false);
ALTER TABLE veiculo ALTER COLUMN id SET DEFAULT nextval('veiculo_id_seq');
ALTER SEQUENCE veiculo_id_seq OWNED BY veiculo.id;

ALTER TABLE veiculo ADD COLUMN descricao VARCHAR(255) NOT NULL DEFAULT '';
