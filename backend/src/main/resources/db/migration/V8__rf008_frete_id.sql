CREATE SEQUENCE frete_id_seq OWNED BY frete.id;
SELECT setval('frete_id_seq', COALESCE(MAX(id), 0) + 1, false) FROM frete;
ALTER TABLE frete ALTER COLUMN id SET DEFAULT nextval('frete_id_seq');
