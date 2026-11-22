-- RF015: IDs gerados, imagem binária compatível com URLs legadas e frete opcional.
CREATE SEQUENCE achado_perdido_id_seq OWNED BY achado_perdido.id;
SELECT setval(
    'achado_perdido_id_seq',
    COALESCE((SELECT MAX(id) FROM achado_perdido), 0) + 1,
    false
);
ALTER TABLE achado_perdido
    ALTER COLUMN id SET DEFAULT nextval('achado_perdido_id_seq'),
    ADD COLUMN id_frete BIGINT NULL,
    ADD CONSTRAINT achado_perdido_frete_fk FOREIGN KEY (id_frete) REFERENCES frete(id);

CREATE INDEX achado_perdido_frete_idx ON achado_perdido (id_frete);

CREATE SEQUENCE foto_item_perdido_id_seq OWNED BY foto_item_perdido.id;
SELECT setval(
    'foto_item_perdido_id_seq',
    COALESCE((SELECT MAX(id) FROM foto_item_perdido), 0) + 1,
    false
);
ALTER TABLE foto_item_perdido
    ALTER COLUMN id SET DEFAULT nextval('foto_item_perdido_id_seq'),
    ALTER COLUMN url DROP NOT NULL,
    ADD COLUMN conteudo BYTEA,
    ADD COLUMN tipo VARCHAR(32),
    ADD CONSTRAINT foto_item_perdido_representacao_ck CHECK (
        (url IS NOT NULL AND conteudo IS NULL AND tipo IS NULL)
        OR (url IS NULL AND conteudo IS NOT NULL AND tipo IS NOT NULL)
    );
