# Spec Delta

## Purpose

Permitir que o motorista responsável registre a conclusão do frete e que o cliente contratante consulte o resultado.

## ADDED Requirements

### Requirement: Registro pelo motorista responsável

O sistema SHALL aceitar a conclusão somente do motorista designado autenticado e somente enquanto o frete estiver em execução.

#### Scenario: Estado incompatível
- **WHEN** o motorista tenta concluir um frete ainda não iniciado, interrompido, cancelado, encerrado sem conclusão ou já concluído
- **THEN** o sistema rejeita a operação e preserva os registros existentes.

#### Scenario: Sessão ausente ou sem autorização
- **WHEN** a tentativa vem de uma sessão ausente ou de uma conta que não corresponde ao motorista atualmente designado
- **THEN** o sistema rejeita a operação sem modificar o frete.

### Requirement: Dados de conclusão por modalidade

O sistema SHALL exigir data e horário informados da conclusão do transporte de pessoas ou da entrega da encomenda e registrar o autor autenticado. Em encomendas, SHALL exigir também nome e CPF do recebedor.

#### Scenario: Concluir deslocamento de pessoas
- **WHEN** o motorista responsável informa a data e o horário da conclusão e confirma a realização do transporte
- **THEN** o sistema registra a data e o horário informados e o autor sem exigir dados de recebedor, foto ou assinatura digital.

#### Scenario: Data ou horário da conclusão de pessoas ausente ou inválido
- **WHEN** os dados obrigatórios da conclusão do transporte de pessoas estiverem ausentes ou inválidos
- **THEN** o sistema indica a falha de validação e preserva a situação do frete.

#### Scenario: Concluir frete de encomenda
- **WHEN** o motorista responsável registra a conclusão com data e horário válidos da entrega e nome e CPF válidos do recebedor
- **THEN** o sistema registra a entrega sem exigir foto ou assinatura digital.

#### Scenario: Dados de entrega ausentes ou inválidos
- **WHEN** os dados obrigatórios da entrega estiverem ausentes ou inválidos
- **THEN** o sistema indica a falha de validação e preserva a situação do frete.

### Requirement: Efeitos da conclusão

O sistema SHALL atualizar a situação para `CONCLUIDO`, acrescentar um evento `CONCLUSAO` ao histórico imutável com autor e horário e gravar uma notificação interna para o cliente contratante.

#### Scenario: Conclusão bem-sucedida
- **WHEN** uma conclusão é registrada com sucesso
- **THEN** o frete consta como concluído no histórico do cliente e do prestador responsável, o evento aparece no histórico de execução e a notificação referencia o frete.

#### Scenario: Envios concorrentes
- **WHEN** dois envios tentam concluir o mesmo frete simultaneamente
- **THEN** apenas um registro de conclusão, um evento e uma notificação são gravados.

#### Scenario: Falha de persistência
- **WHEN** a atualização do frete, a inclusão do evento ou a gravação da notificação falha
- **THEN** a operação falha e nenhum dos três efeitos é persistido.

### Requirement: Consulta e elegibilidade para contestação

O sistema SHALL permitir que o cliente contratante consulte a conclusão do próprio frete e disponibilizar a situação concluída para a elegibilidade da US-015.

#### Scenario: Consulta pelo cliente contratante
- **WHEN** o cliente consulta a conclusão do próprio frete
- **THEN** o sistema apresenta os dados autorizados da conclusão e identifica o frete como elegível para contestação.

#### Scenario: Consulta sem vínculo autorizado
- **WHEN** uma conta sem vínculo autorizado solicita dados da conclusão
- **THEN** o sistema rejeita a consulta sem expor os dados do frete ou do recebedor.
