# RF008 — Confirmação de Contratação

## Why

O RF007 já registra propostas de prestadores. O RF008 fecha o fluxo do cliente, que precisa comparar as propostas da sua solicitação e contratar um único prestador, conforme US-008, RU003 e RN010. A referência oficial é o documento de requisitos em `origin/documents`.

## What Changes

- Consultar prestador, valor e condições das propostas recebidas de uma solicitação de pessoas ou encomendas.
- Aceitar uma única proposta, registrar o acordo e permitir ao cliente consultar o frete criado em `AGUARDANDO_DESIGNACAO`.
- Registrar a notificação interna ao prestador escolhido. A central de notificações, a designação de motorista e veículo e a execução do frete pertencem aos respectivos requisitos.

## Capabilities

### New Capabilities

- `confirmacao-de-contratacao` — Consulta de propostas pelo cliente, confirmação da contratação e consulta do frete contratado.

### Modified Capabilities

## Impact

- Consome propostas e solicitações persistidas no PostgreSQL. O RF007 mantém seu contrato e continua impedindo novos envios após o encerramento da solicitação.
- Prevê operações autenticadas no Spring Boot, migration Flyway para geração de IDs do frete e integração com o router e a navegação React. A entrada provisória é Minhas viagens, sujeita à confirmação pelo Figma.
- Coordenar o router, o cancelamento concorrente e a numeração de migrations com o RF011. A implementação do backend requer autorização explícita conforme o AGENTS.md.
