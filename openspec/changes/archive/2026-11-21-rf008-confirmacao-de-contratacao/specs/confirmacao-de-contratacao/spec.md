# RF008 — Confirmação de Contratação

## Purpose

Permitir ao cliente comparar propostas recebidas, confirmar um único acordo por solicitação e consultar o frete contratado.

## ADDED Requirements

### Requirement: Consulta das propostas recebidas

O sistema SHALL permitir ao cliente autenticado consultar as propostas da sua solicitação de pessoas ou encomendas, com os dados públicos do prestador, avaliação quando disponível, valor e condições.

#### Scenario: Comparar propostas recebidas
- **WHEN** o titular consulta uma solicitação que recebeu propostas
- **THEN** o sistema apresenta as propostas com seus respectivos prestadores, valores e condições.

#### Scenario: Solicitação sem propostas
- **WHEN** o titular consulta uma solicitação que ainda não recebeu propostas
- **THEN** o sistema indica que ainda não há propostas para comparar.

### Requirement: Aceite único e registro da contratação

O sistema SHALL permitir ao titular aceitar uma proposta elegível da própria solicitação aberta, registrar o acordo e encerrar a solicitação para novos envios e aceites.

#### Scenario: Confirmar uma proposta
- **WHEN** o titular confirma uma proposta enviada para a sua solicitação aberta
- **THEN** o sistema marca a solicitação como contratada e cria um frete vinculado à solicitação, à proposta e ao prestador, com o valor e as condições acordadas, em `AGUARDANDO_DESIGNACAO` e sem designação.
- **AND** preserva as demais propostas com seus estados anteriores e impede seu aceite.

#### Scenario: Aceites concorrentes de propostas distintas
- **WHEN** o titular tenta aceitar duas propostas da mesma solicitação simultaneamente
- **THEN** uma confirmação tem sucesso e a outra recebe um conflito, com apenas um frete e uma notificação de contratação registrados.

#### Scenario: Repetição do aceite vencedor
- **WHEN** o titular repete o aceite da proposta que originou o frete
- **THEN** o sistema retorna a referência do frete existente sem duplicar o registro ou a notificação.

#### Scenario: Solicitação ou proposta inelegível
- **WHEN** o titular tenta aceitar uma proposta de outra solicitação, uma proposta inelegível ou uma proposta de solicitação encerrada
- **THEN** o sistema rejeita o aceite sem produzir uma nova contratação ou notificação.

### Requirement: Consulta do frete contratado

O sistema SHALL permitir ao cliente autenticado consultar seu frete contratado e verificar o prestador responsável, o valor e as condições vigentes e a situação do serviço.

#### Scenario: Consultar após a confirmação
- **WHEN** o titular abre o frete recém-contratado
- **THEN** o sistema apresenta os dados persistidos do frete e permite consultar o detalhe novamente após recarregar.

### Requirement: Acesso restrito ao titular

O sistema SHALL restringir as operações de cliente do RF008 à sessão autenticada do titular da solicitação.

#### Scenario: Acesso sem vínculo com a solicitação
- **WHEN** uma sessão ausente ou de outro usuário tenta listar propostas, aceitar uma delas ou consultar o frete pelo fluxo do cliente
- **THEN** o sistema rejeita a operação sem expor dados da solicitação ou modificar seus registros.

### Requirement: Notificação interna da confirmação

O sistema SHALL registrar uma notificação interna para o prestador escolhido como parte da confirmação da contratação.

#### Scenario: Notificar o prestador contratado
- **WHEN** uma contratação é confirmada com sucesso
- **THEN** o prestador escolhido recebe um registro de notificação associado ao frete e à solicitação.

#### Scenario: Falha no registro da notificação
- **WHEN** a notificação não pode ser gravada durante a confirmação
- **THEN** a operação falha sem persistir o aceite, a mudança da solicitação ou o novo frete.
