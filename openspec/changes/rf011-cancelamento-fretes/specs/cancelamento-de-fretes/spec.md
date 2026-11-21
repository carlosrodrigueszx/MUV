# Spec Delta

## Purpose

Permitir o cancelamento de um frete contratado antes da execução, com rastreabilidade e aviso à outra parte.

## ADDED Requirements

### Requirement: Autorizar consulta e confirmação de cancelamento

O sistema SHALL identificar o autor pela sessão e restringir a consulta e a confirmação ao cliente ou prestador responsável pelo frete. A confirmação SHALL exigir token CSRF válido.

#### Scenario: Acesso sem sessão, sem vínculo ou sem CSRF

- **WHEN** uma conta sem vínculo consulta ou confirma o cancelamento, a sessão está ausente ou a confirmação tem token CSRF ausente ou inválido
- **THEN** o sistema rejeita a operação e preserva o frete e seus registros.

### Requirement: Validar situação e justificativa ao confirmar

O sistema SHALL permitir o cancelamento apenas em `AGUARDANDO_DESIGNACAO` e `PRONTO_INICIO`, com justificativa preenchida e confirmação explícita na interface.

#### Scenario: Justificativa sem conteúdo

- **WHEN** o autor envia uma justificativa ausente, vazia ou somente com caracteres de espaço
- **THEN** o sistema rejeita a confirmação sem realizar gravações.

#### Scenario: Situação mudou depois de abrir a tela

- **WHEN** a execução já iniciou, inclusive se estiver interrompida, ou o frete está em outro estado inelegível na hora da gravação
- **THEN** o sistema rejeita o cancelamento e mantém a situação atual.

### Requirement: Persistir e apresentar o cancelamento

O sistema SHALL gravar situação, evento e notificação atomicamente e preservar as condições financeiras contratadas.

#### Scenario: Cliente ou prestador cancela frete elegível

- **WHEN** o cliente ou prestador responsável confirma o cancelamento com justificativa válida
- **THEN** o sistema muda a situação para `CANCELADO`, registra autor, data, hora e justificativa em `CANCELAMENTO` e registra uma notificação interna à outra parte
- **AND** disponibiliza ao usuário autorizado o estado e o evento persistidos
- **AND** preserva valor e condições e não aplica multa, cobrança ou reembolso automático.

#### Scenario: Falha em uma escrita

- **WHEN** uma falha impede a gravação do estado, do evento ou da notificação
- **THEN** a transação é revertida e nenhum dos três efeitos permanece.

#### Scenario: Reenvio ou confirmação simultânea

- **WHEN** o frete já foi cancelado ou duas confirmações disputam o mesmo frete
- **THEN** somente o primeiro cancelamento bem-sucedido produz um evento e uma notificação.

#### Scenario: Transição concorrente

- **WHEN** uma designação, substituição ou início de execução disputa a linha do frete com um cancelamento
- **THEN** cada comando reavalia o estado após adquirir o bloqueio e aplica apenas uma transição permitida pelo estado vigente
- **AND** a execução iniciada impede cancelamento e o frete cancelado impede novas transições de designação ou início.

### Requirement: Oferecer cancelamento a partir do frete contratado

A interface SHALL ligar a consulta de frete contratado à confirmação pelo identificador real do frete elegível.

#### Scenario: Origem da ação

- **WHEN** o usuário consulta um frete contratado elegível
- **THEN** a interface oferece a ação “Cancelar frete” para aquele frete
- **AND** uma ação direta no histórico depende de identificação inequívoca do frete e de estado elegível fornecido pelo contrato.
