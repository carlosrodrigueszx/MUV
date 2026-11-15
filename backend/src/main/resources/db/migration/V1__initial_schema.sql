CREATE TABLE Usuario
(
    id BIGINT NOT NULL,
    nome VARCHAR(255) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    senha VARCHAR(255) NOT NULL,            -- hash da senha
    foto_url VARCHAR(512),                  -- opcional: o cadastro (RF001) não exige foto
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (email)                          -- login por e-mail (RF001)
);

CREATE TABLE Passageiro
(
    id BIGINT NOT NULL,
    cpf VARCHAR(14) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (id) REFERENCES Usuario(id),
    UNIQUE (cpf)
);

CREATE TABLE Prestador
(
    id BIGINT NOT NULL,
    avaliacao_media DECIMAL(3,2) NOT NULL DEFAULT 0,
    total_avaliacoes INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    FOREIGN KEY (id) REFERENCES Usuario(id)
);

CREATE TABLE Prestador_Autonomo
(
    id BIGINT NOT NULL,
    cpf VARCHAR(14) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (id) REFERENCES Prestador(id),
    UNIQUE (cpf)
);

CREATE TABLE Prestador_Empresa
(
    id BIGINT NOT NULL,
    cnpj VARCHAR(18) NOT NULL,
    razao_social VARCHAR(255) NOT NULL,
    nome_fantasia VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (id) REFERENCES Prestador(id),
    UNIQUE (cnpj)
);

-- Motorista independe de uma única empresa: o vínculo e a verificação
-- ficam em Vinculo_Motorista (US-010: um motorista pode ter vários vínculos,
-- cada organização faz a sua própria verificação).
CREATE TABLE Motorista
(
    id BIGINT NOT NULL,
    nome VARCHAR(255) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    cnh VARCHAR(20) NOT NULL,
    categoria_cnh VARCHAR(5),
    usuario_id BIGINT,                      -- conta de acesso, para o motorista registrar a execução (US-012)
    PRIMARY KEY (id),
    FOREIGN KEY (usuario_id) REFERENCES Usuario(id),
    UNIQUE (cnh),
    UNIQUE (usuario_id)
);

CREATE TABLE Vinculo_Motorista
(
    prestador_id BIGINT NOT NULL,
    motorista_id BIGINT NOT NULL,
    status_verificacao VARCHAR(28) NOT NULL DEFAULT 'PENDENTE',
    verificado_por_id BIGINT,
    data_verificacao TIMESTAMP NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (prestador_id, motorista_id),
    FOREIGN KEY (prestador_id) REFERENCES Prestador(id),
    FOREIGN KEY (motorista_id) REFERENCES Motorista(id),
    FOREIGN KEY (verificado_por_id) REFERENCES Usuario(id)
);

CREATE TABLE Veiculo
(
    id BIGINT NOT NULL,
    modelo VARCHAR(32) NOT NULL,
    ano SMALLINT NOT NULL,                  -- só o ano; DATE exigiria dia e mês
    cor VARCHAR(32) NOT NULL,
    placa VARCHAR(8) NOT NULL,              -- 7 caracteres, ou 8 se gravar com hífen
    capacidade_passageiros INT NOT NULL DEFAULT 0,
    capacidade_carga_kg DECIMAL(10,2) NOT NULL DEFAULT 0,   -- (5,2) limitava a 999,99 kg
    marca VARCHAR(64) NOT NULL,
    modalidade VARCHAR(12) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    id_prestador BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (id_prestador) REFERENCES Prestador(id),
    UNIQUE (placa),
    UNIQUE (id_prestador, id),              -- permite a FK composta de Frete (veículo pertence ao prestador)
    CHECK (modalidade IN ('PESSOA','ENCOMENDA','AMBOS'))
);

CREATE TABLE Foto_Veiculo
(
    id BIGINT NOT NULL,
    url VARCHAR(255) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_veiculo BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (id_veiculo) REFERENCES Veiculo(id)
);

-- ---------------------------------------------------------------------
-- 2. Autorização e regiões de atuação (RN004, RF002)
-- ---------------------------------------------------------------------

-- Uma linha por modalidade solicitada; o admin aprova/rejeita cada uma separadamente.
CREATE TABLE Autorizacao_Modalidade
(
    id BIGINT NOT NULL,
    modalidade VARCHAR(12) NOT NULL,
    status VARCHAR(28) NOT NULL DEFAULT 'PENDENTE',
    data_solicitacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_autorizacao TIMESTAMP NULL,        -- vazio enquanto pendente
    id_prestador BIGINT NOT NULL,
    id_autorizador BIGINT,                  -- vazio enquanto pendente
    PRIMARY KEY (id),
    FOREIGN KEY (id_prestador) REFERENCES Prestador(id),
    FOREIGN KEY (id_autorizador) REFERENCES Usuario(id),
    UNIQUE (id_prestador, modalidade),
    CHECK (modalidade IN ('PESSOA','ENCOMENDA'))
);

CREATE TABLE Regiao_Atuacao
(
    id BIGINT NOT NULL,
    cidade VARCHAR(128) NOT NULL,
    estado CHAR(2) NOT NULL,
    id_prestador BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (id_prestador) REFERENCES Prestador(id),
    UNIQUE (id_prestador, cidade, estado)
);

-- ---------------------------------------------------------------------
-- 3. Solicitações e propostas (RN001, RN006-RN010)
-- ---------------------------------------------------------------------

CREATE TABLE Endereco
(
    id BIGINT NOT NULL,
    cep VARCHAR(9) NOT NULL,
    logradouro VARCHAR(128) NOT NULL,
    numero VARCHAR(20) NOT NULL,
    complemento VARCHAR(128),
    bairro VARCHAR(128) NOT NULL,
    cidade VARCHAR(128) NOT NULL,
    estado CHAR(2) NOT NULL,
    ponto_referencia VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE Solicitacao_Frete
(
    id BIGINT NOT NULL,
    modalidade VARCHAR(12) NOT NULL,
    data_hora_pretendida TIMESTAMP NOT NULL,
    status VARCHAR(28) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_passageiro BIGINT NOT NULL,
    id_prestador BIGINT,                    -- só preenchido em solicitação DIRECIONADA; NULL = aberta (RF006/RF007)
    endereco_origem_id BIGINT NOT NULL,
    endereco_destino_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (id_passageiro) REFERENCES Passageiro(id),
    FOREIGN KEY (id_prestador) REFERENCES Prestador(id),
    FOREIGN KEY (endereco_origem_id) REFERENCES Endereco(id),
    FOREIGN KEY (endereco_destino_id) REFERENCES Endereco(id),
    CHECK (modalidade IN ('PESSOA','ENCOMENDA'))   -- RN001: uma única modalidade por solicitação
);

CREATE TABLE Solicitacao_Pessoa
(
    id BIGINT NOT NULL,
    quantidade_pessoas INT NOT NULL,
    possui_bagagem BOOLEAN NOT NULL DEFAULT FALSE,
    descricao_bagagens VARCHAR(255),
    PRIMARY KEY (id),
    FOREIGN KEY (id) REFERENCES Solicitacao_Frete(id),
    CHECK (quantidade_pessoas > 0)
);

CREATE TABLE Solicitacao_Encomenda
(
    id BIGINT NOT NULL,
    descricao_volumes VARCHAR(255) NOT NULL,
    peso_estimado_kg DECIMAL(10,2) NOT NULL,
    altura_cm DECIMAL(7,2) NOT NULL,
    largura_cm DECIMAL(7,2) NOT NULL,
    profundidade_cm DECIMAL(7,2) NOT NULL,
    nome_contato_coleta VARCHAR(100) NOT NULL,
    telefone_contato_coleta VARCHAR(20) NOT NULL,
    instrucoes_coleta VARCHAR(255),
    nome_contato_entrega VARCHAR(100) NOT NULL,
    telefone_contato_entrega VARCHAR(20) NOT NULL,
    instrucoes_entrega VARCHAR(255),
    PRIMARY KEY (id),
    FOREIGN KEY (id) REFERENCES Solicitacao_Frete(id),
    CHECK (peso_estimado_kg > 0)
);

CREATE TABLE Proposta
(
    id BIGINT NOT NULL,
    status VARCHAR(28) NOT NULL,
    valor DECIMAL(13,2) NOT NULL,
    condicoes TEXT NOT NULL,                -- RN009 / RF007: a proposta traz valor E condições
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,   -- era DATE: perdia a hora
    id_solicitacao BIGINT NOT NULL,
    id_prestador BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (id_solicitacao) REFERENCES Solicitacao_Frete(id),
    FOREIGN KEY (id_prestador) REFERENCES Prestador(id),
    CHECK (valor > 0)
);

-- ---------------------------------------------------------------------
-- 4. Frete, acordos de alteração e histórico (RN011-RN016)
-- ---------------------------------------------------------------------

CREATE TABLE Frete
(
    id BIGINT NOT NULL,
    valor_atual DECIMAL(13,2) NOT NULL,
    condicoes_atuais TEXT,                  -- condições vigentes (mudam se um novo acordo for aceito)
    status VARCHAR(28) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,   -- data da contratação
    solicitacao_id BIGINT NOT NULL,
    proposta_aceita_id BIGINT NOT NULL,
    prestador_responsavel_id BIGINT NOT NULL,
    motorista_atual_id BIGINT,              -- NULL até a designação (US-011)
    veiculo_atual_id BIGINT,                -- NULL até a designação (US-011)
    data_hora_conclusao TIMESTAMP NULL,     -- NULL até a conclusão
    responsavel_registro_conclusao_id BIGINT,
    nome_destinatario VARCHAR(128),         -- RN016: quem recebeu (encomendas)
    cpf_destinatario VARCHAR(14),
    PRIMARY KEY (id),
    FOREIGN KEY (solicitacao_id) REFERENCES Solicitacao_Frete(id),
    FOREIGN KEY (proposta_aceita_id) REFERENCES Proposta(id),
    FOREIGN KEY (prestador_responsavel_id) REFERENCES Prestador(id),
    FOREIGN KEY (responsavel_registro_conclusao_id) REFERENCES Usuario(id),
    -- RN010: uma solicitação só gera um frete, e uma proposta só é aceita uma vez
    UNIQUE (solicitacao_id),
    UNIQUE (proposta_aceita_id),
    -- RF003/RF009: só se designa motorista VINCULADO ao prestador responsável
    FOREIGN KEY (prestador_responsavel_id, motorista_atual_id)
        REFERENCES Vinculo_Motorista(prestador_id, motorista_id),
    -- RN012: o veículo precisa pertencer ao prestador responsável
    FOREIGN KEY (prestador_responsavel_id, veiculo_atual_id)
        REFERENCES Veiculo(id_prestador, id),
    CHECK (status IN ('AGUARDANDO_DESIGNACAO','PRONTO_INICIO','EM_EXECUCAO','INTERROMPIDO',
                      'CONCLUIDO','CANCELADO','ENCERRADO_SEM_CONCLUSAO')),
    -- RN012 / US-012: não inicia sem motorista e veículo
    CHECK (status IN ('AGUARDANDO_DESIGNACAO','CANCELADO')
        OR (motorista_atual_id IS NOT NULL AND veiculo_atual_id IS NOT NULL)),
    -- RN016: concluído exige data/hora e responsável pelo registro
    CHECK (status <> 'CONCLUIDO'
        OR (data_hora_conclusao IS NOT NULL AND responsavel_registro_conclusao_id IS NOT NULL))
);

CREATE TABLE Solicitacao_Alteracao_Acordo
(
    id BIGINT NOT NULL,
    novo_valor DECIMAL(13,2),
    novas_condicoes TEXT NOT NULL,
    justificativa TEXT NOT NULL,
    status VARCHAR(28) NOT NULL,
    data_hora_solicitacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_hora_resposta TIMESTAMP NULL,      -- NULL enquanto aguarda o cliente
    id_frete BIGINT NOT NULL,
    prestador_solicitante_id BIGINT NOT NULL,
    novo_motorista_id BIGINT,               -- só o que está sendo trocado
    novo_veiculo_id BIGINT,
    PRIMARY KEY (id),
    FOREIGN KEY (id_frete) REFERENCES Frete(id),
    FOREIGN KEY (prestador_solicitante_id) REFERENCES Prestador(id),
    FOREIGN KEY (novo_motorista_id) REFERENCES Motorista(id),
    FOREIGN KEY (novo_veiculo_id) REFERENCES Veiculo(id)
);

-- Registro imutável (somente INSERT): RF010, RN013, RN014, RN015.
CREATE TABLE Historico_Execucao_Frete
(
    id BIGINT NOT NULL,
    frete_id BIGINT NOT NULL,
    usuario_autor_id BIGINT NOT NULL,
    tipo_evento VARCHAR(28) NOT NULL,
    descricao TEXT,                         -- obrigatória em OCORRENCIA (US-012)
    justificativa TEXT,                     -- obrigatória conforme o CHECK abaixo
    motorista_id BIGINT,
    veiculo_id BIGINT,
    solicitacao_alteracao_id BIGINT,        -- liga SUBSTITUICAO_COM_ACORDO ao acordo aceito
    data_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (frete_id) REFERENCES Frete(id),
    FOREIGN KEY (usuario_autor_id) REFERENCES Usuario(id),
    FOREIGN KEY (motorista_id) REFERENCES Motorista(id),
    FOREIGN KEY (veiculo_id) REFERENCES Veiculo(id),
    FOREIGN KEY (solicitacao_alteracao_id) REFERENCES Solicitacao_Alteracao_Acordo(id),
    CHECK (tipo_evento IN ('DESIGNACAO','SUBSTITUICAO_SEM_ALTERACAO','SUBSTITUICAO_COM_ACORDO',
                           'INICIO','OCORRENCIA','INTERRUPCAO','RETOMADA','CANCELAMENTO',
                           'CONCLUSAO','ENCERRAMENTO_SEM_CONCLUSAO')),
    CHECK (tipo_evento NOT IN ('INTERRUPCAO','CANCELAMENTO','ENCERRAMENTO_SEM_CONCLUSAO',
                               'SUBSTITUICAO_SEM_ALTERACAO','SUBSTITUICAO_COM_ACORDO')
        OR (justificativa IS NOT NULL AND CHAR_LENGTH(TRIM(justificativa)) > 0)),
    CHECK (tipo_evento <> 'OCORRENCIA' OR descricao IS NOT NULL),
    CHECK (tipo_evento <> 'DESIGNACAO' OR (motorista_id IS NOT NULL AND veiculo_id IS NOT NULL)),
    CHECK (tipo_evento NOT IN ('SUBSTITUICAO_SEM_ALTERACAO','SUBSTITUICAO_COM_ACORDO')
        OR motorista_id IS NOT NULL OR veiculo_id IS NOT NULL)
);

CREATE TABLE Avaliacao
(
    id BIGINT NOT NULL,
    nota SMALLINT NOT NULL,                 -- INT(Decimal) não existe em nenhum SGBD
    comentario VARCHAR(255),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_passageiro BIGINT NOT NULL,
    id_prestador BIGINT NOT NULL,           -- quem está sendo avaliado
    id_frete BIGINT NOT NULL,               -- uma avaliação por frete
    PRIMARY KEY (id),
    FOREIGN KEY (id_passageiro) REFERENCES Passageiro(id),
    FOREIGN KEY (id_prestador) REFERENCES Prestador(id),
    FOREIGN KEY (id_frete) REFERENCES Frete(id),
    UNIQUE (id_frete),
    CHECK (nota BETWEEN 1 AND 5)
);

-- ---------------------------------------------------------------------
-- 5. Notificações, achados e perdidos, recuperação de senha
-- ---------------------------------------------------------------------

CREATE TABLE Notificacao
(
    id BIGINT NOT NULL,
    titulo VARCHAR(64) NOT NULL,
    mensagem VARCHAR(255) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lida BOOLEAN NOT NULL DEFAULT FALSE,
    usuario_destino_id BIGINT NOT NULL,
    frete_id BIGINT,                        -- NULL: notificação anterior à contratação
    solicitacao_id BIGINT,                  -- propostas recebidas, solicitação direcionada (RF013)
    PRIMARY KEY (id),
    FOREIGN KEY (usuario_destino_id) REFERENCES Usuario(id),
    FOREIGN KEY (frete_id) REFERENCES Frete(id),
    FOREIGN KEY (solicitacao_id) REFERENCES Solicitacao_Frete(id),
    CHECK (frete_id IS NOT NULL OR solicitacao_id IS NOT NULL)
);

CREATE TABLE Achado_Perdido
(
    id BIGINT NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    data_encontrado TIMESTAMP NOT NULL,
    status VARCHAR(28) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_veiculo BIGINT NOT NULL,
    id_registrador BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (id_veiculo) REFERENCES Veiculo(id),
    FOREIGN KEY (id_registrador) REFERENCES Usuario(id)
);

CREATE TABLE Foto_Item_Perdido
(
    id BIGINT NOT NULL,
    url VARCHAR(255) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_item BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (id_item) REFERENCES Achado_Perdido(id)
);

CREATE TABLE Token_Recuperacao
(
    id BIGINT NOT NULL,
    token VARCHAR(256) NOT NULL,            -- ideal: gravar o hash do token
    expira_em TIMESTAMP NOT NULL,
    usado_em TIMESTAMP NULL,                -- NULL = ainda não usado
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (id_usuario) REFERENCES Usuario(id),
    UNIQUE (token)
);

-- ---------------------------------------------------------------------
-- 6. Índices (PostgreSQL não indexa FKs automaticamente)
-- ---------------------------------------------------------------------
CREATE INDEX idx_historico_frete      ON Historico_Execucao_Frete (frete_id, data_hora);
CREATE INDEX idx_proposta_solicitacao ON Proposta (id_solicitacao);
CREATE INDEX idx_solicitacao_busca    ON Solicitacao_Frete (status, modalidade);
CREATE INDEX idx_notificacao_usuario  ON Notificacao (usuario_destino_id, lida);
CREATE INDEX idx_frete_prestador      ON Frete (prestador_responsavel_id, status);