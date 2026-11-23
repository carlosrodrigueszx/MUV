# Proposal

## Why

O RF012 permite que o motorista responsável registre o término de um frete em execução e que o cliente seja informado e possa consultar a conclusão. Para encomendas, o registro identifica quem recebeu a entrega e quando ela ocorreu.

## What Changes

- Registrar a conclusão de fretes de pessoas e encomendas, conforme RF012, US-013 e RN016 do documento oficial na branch `documents`.
- Atualizar a situação para `CONCLUIDO`, acrescentar o evento ao histórico de execução e notificar o cliente contratante.
- Disponibilizar a consulta autorizada da conclusão e o estado de elegibilidade para a contestação prevista na US-015.

## Capabilities

### New Capabilities

- `registro-conclusao-de-frete` — Registro e consulta da conclusão do frete.

### Modified Capabilities

## Impact

- Spring Boot e persistência PostgreSQL via JDBC, com reaproveitamento do esquema de fretes, histórico e notificações.
- Dependências de integração com RF001, RF008, RF009, RF010, RF013 e RF014. A gestão de contestações pertence à US-015.
- Interface React com formulários de registro específicos para pessoas e encomendas, referenciados pelos PNGs e JSONs em `docs/interface/registrar-conclusao-frete`. A consulta pelo cliente ainda requer referência visual. A execução das tarefas de backend depende da autorização prevista no AGENTS.md.
