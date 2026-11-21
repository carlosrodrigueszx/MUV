# Spec Delta

## Purpose

Garantir que cada frete contratado possua motorista e veículo elegíveis, compatíveis e rastreáveis antes da execução, preservando as condições acordadas e o acesso dos envolvidos.

## ADDED Requirements

### Requirement: Consultar contexto e recursos elegíveis
O sistema SHALL permitir que somente o prestador responsável consulte o frete contratado e os recursos disponíveis para designação.

#### Scenario: Organização consulta recursos elegíveis
- **WHEN** o gestor autenticado abre um frete contratado sob responsabilidade de sua organização
- **THEN** o sistema apresenta os dados do serviço, os motoristas vinculados e verificados pela organização e os veículos ativos pertencentes a ela
- **AND** não expõe vínculos, verificações ou veículos de outro prestador.

#### Scenario: Prestador diferente tenta consultar o frete
- **WHEN** um prestador autenticado consulta um frete contratado sob responsabilidade de outro prestador
- **THEN** o sistema rejeita o acesso sem revelar os recursos ou detalhes protegidos do frete.

### Requirement: Validar a compatibilidade dos recursos
O sistema MUST revalidar no servidor a elegibilidade e a compatibilidade do motorista e do veículo no momento de cada designação ou substituição.

#### Scenario: Veículo compatível com transporte de pessoas
- **WHEN** o prestador seleciona veículo ativo, próprio, habilitado para pessoas e com capacidade igual ou superior à quantidade de passageiros
- **THEN** o veículo é considerado compatível para essa solicitação.

#### Scenario: Veículo compatível com transporte de encomenda
- **WHEN** o prestador seleciona veículo ativo, próprio, habilitado para encomendas e com capacidade em quilogramas igual ou superior ao peso estimado
- **THEN** o veículo é considerado compatível para essa solicitação.

#### Scenario: Recurso incompatível
- **WHEN** o motorista não está elegível ou o veículo está inativo, pertence a outro prestador, não atende à modalidade ou possui capacidade insuficiente
- **THEN** o sistema rejeita a operação, informa a incompatibilidade e conserva a designação e a situação anteriores do frete.

#### Scenario: Critério não representado no cadastro
- **WHEN** a interface apresenta volume, categoria veicular ou disponibilidade de agenda sem que o cadastro possua dados suficientes para validar esse critério
- **THEN** o sistema não declara o recurso compatível com base nesse critério nem inventa valores para completar a validação.

### Requirement: Designar motorista e veículo
O sistema SHALL registrar uma designação completa para frete aguardando recursos e torná-lo pronto para início somente após todas as validações.

#### Scenario: Designação inicial válida
- **WHEN** o prestador responsável confirma motorista e veículo elegíveis para um frete em `AGUARDANDO_DESIGNACAO`
- **THEN** o sistema grava os dois recursos como designação atual, muda o frete para `PRONTO_INICIO` e registra um evento `DESIGNACAO` com autor, recursos, data e horário
- **AND** essas alterações ficam disponíveis de forma atômica.

#### Scenario: Designação incompleta
- **WHEN** a operação não informa motorista ou veículo
- **THEN** o sistema rejeita a designação e mantém o frete em `AGUARDANDO_DESIGNACAO`.

#### Scenario: Frete inelegível para designação inicial
- **WHEN** o prestador tenta realizar a primeira designação em frete cancelado, concluído, encerrado ou cuja execução já começou
- **THEN** o sistema rejeita a operação sem modificar o frete ou seu histórico.

### Requirement: Identificar o motorista autônomo responsável
O sistema SHALL tratar o prestador autônomo contratado como motorista da própria execução e permitir que ele escolha somente veículo elegível pertencente ao seu cadastro.

#### Scenario: Autônomo realiza a designação inicial
- **WHEN** um prestador autônomo abre seu frete em `AGUARDANDO_DESIGNACAO` e seleciona veículo compatível
- **THEN** o sistema identifica o próprio autônomo como motorista, grava a designação completa e torna o frete pronto para início.

#### Scenario: Autônomo tenta escolher outro motorista sem novo acordo
- **WHEN** o autônomo tenta substituir o profissional responsável por outra pessoa sem acordo aceito pelo cliente
- **THEN** o sistema não altera a designação vigente.

### Requirement: Bloquear execução sem designação válida
O sistema MUST impedir o início do frete enquanto motorista e veículo elegíveis não estiverem designados.

#### Scenario: Tentativa de início aguardando designação
- **WHEN** um usuário autorizado tenta iniciar um frete em `AGUARDANDO_DESIGNACAO` ou com um dos recursos ausente
- **THEN** o sistema rejeita o início e mantém a situação do frete.

#### Scenario: Designação válida libera a etapa de execução
- **WHEN** o frete possui motorista e veículo válidos e está em `PRONTO_INICIO`
- **THEN** a interface informa que os recursos estão designados e disponibiliza a etapa de início ao responsável autorizado pelo RF010.

### Requirement: Substituir recursos sem alterar as condições
O sistema SHALL permitir substituição justificada em frete pronto para início ou interrompido quando os novos recursos mantiverem as condições contratadas.

#### Scenario: Substituição válida antes do início
- **WHEN** o prestador responsável informa justificativa e confirma novos recursos elegíveis para frete em `PRONTO_INICIO`
- **THEN** o sistema substitui os recursos indicados, conserva valor e condições, registra `SUBSTITUICAO_SEM_ALTERACAO` e notifica o cliente
- **AND** o histórico permite identificar a designação anterior e a resultante.

#### Scenario: Substituição válida durante interrupção
- **WHEN** o frete está `INTERROMPIDO` e o prestador confirma recursos elegíveis sem modificar as condições contratadas
- **THEN** o sistema registra a nova designação e mantém o frete interrompido até uma retomada autorizada.

#### Scenario: Substituição sem justificativa
- **WHEN** o prestador tenta substituir motorista ou veículo sem justificativa preenchida
- **THEN** o sistema rejeita a operação e não altera designação, histórico ou notificações.

#### Scenario: Substituição durante execução ativa
- **WHEN** o prestador tenta substituir recursos enquanto o frete está `EM_EXECUCAO`
- **THEN** o sistema exige que o frete seja interrompido pelo fluxo autorizado antes de aceitar a substituição.

### Requirement: Exigir novo acordo para alteração das condições
O sistema MUST conservar a designação vigente quando a substituição modificar as condições contratadas, até que o cliente aceite um novo acordo.

#### Scenario: Alteração depende do cliente
- **WHEN** o prestador propõe substituição que modifica valor, condições ou o profissional autônomo originalmente contratado
- **THEN** o sistema registra uma solicitação de alteração pendente e notifica o cliente
- **AND** não atualiza os recursos atuais nem as condições vigentes.

#### Scenario: Cliente aceita o novo acordo
- **WHEN** o cliente responsável aceita uma solicitação de alteração pendente cujos novos recursos continuam elegíveis
- **THEN** o sistema aplica recursos e condições acordados, registra `SUBSTITUICAO_COM_ACORDO` e notifica os envolvidos.

#### Scenario: Cliente rejeita o novo acordo
- **WHEN** o cliente responsável rejeita uma solicitação de alteração pendente
- **THEN** o sistema mantém a designação, o valor e as condições anteriores e registra a resposta.

### Requirement: Consultar a designação pelo cliente
O sistema SHALL permitir que o cliente responsável consulte o motorista e o veículo atualmente designados para seu frete, sem expor dados pessoais desnecessários.

#### Scenario: Cliente consulta o próprio frete
- **WHEN** o cliente autenticado abre o detalhe de um frete contratado por ele
- **THEN** o sistema apresenta nome do motorista e identificação e características pertinentes do veículo atualmente designado.

#### Scenario: Usuário consulta frete alheio
- **WHEN** um cliente ou prestador tenta consultar a designação de frete ao qual não está vinculado
- **THEN** o sistema rejeita o acesso e não expõe informações do motorista ou do veículo.

### Requirement: Preservar consistência sob concorrência
O sistema MUST serializar alterações concorrentes da designação e revalidar recursos e situação do frete dentro da operação de gravação.

#### Scenario: Duas alterações concorrentes
- **WHEN** duas confirmações tentam alterar simultaneamente a designação do mesmo frete
- **THEN** apenas uma alteração baseada no estado vigente é aplicada e a outra recebe conflito para recarregar os dados
- **AND** não são produzidos estado parcial, evento duplicado ou notificação inconsistente.
