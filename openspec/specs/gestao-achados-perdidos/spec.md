# gestao-achados-perdidos Specification

## Purpose

Permite registrar, consultar e acompanhar objetos esquecidos em veículos para apoiar a identificação do proprietário e a devolução do item.

## Requirements

### Requirement: Registrar objeto encontrado
O sistema SHALL permitir que o prestador responsável pelo veículo ou um motorista vinculado a esse prestador registre um objeto encontrado com foto, descrição, data e veículo.

#### Scenario: Prestador registra objeto em veículo próprio
- **WHEN** o prestador autenticado registra um objeto em um veículo pertencente a ele e informa os dados obrigatórios
- **THEN** o sistema cria o registro em situação `EM_GUARDA` e associa o usuário registrador ao item.

#### Scenario: Motorista vinculado registra objeto
- **WHEN** um motorista autenticado vinculado ao prestador responsável registra um objeto em veículo desse prestador
- **THEN** o sistema cria o registro em situação `EM_GUARDA` e associa o motorista como usuário registrador.

#### Scenario: Associar frete quando conhecido
- **WHEN** o registrador informa um frete relacionado ao objeto
- **THEN** o sistema associa esse frete ao registro, desde que pertença ao contexto do prestador responsável pelo veículo.
- **AND** a ausência de frete relacionado não impede o registro.

#### Scenario: Veículo de outro prestador
- **WHEN** o usuário tenta registrar um objeto em veículo que não pertence ao prestador responsável ou ao vínculo de atuação autorizado
- **THEN** o sistema rejeita a operação sem criar o item ou a foto.

### Requirement: Consultar registros do prestador
O sistema SHALL permitir que o prestador responsável e os motoristas vinculados consultem os objetos registrados nos veículos daquele prestador, sem expor registros de outros prestadores.

#### Scenario: Consultar itens do próprio prestador
- **WHEN** um prestador responsável ou motorista vinculado consulta os achados e perdidos
- **THEN** o sistema apresenta os registros dos veículos pertencentes ao prestador, incluindo foto, descrição, data, situação e frete relacionado quando informado.

#### Scenario: Tentar consultar itens de outro prestador
- **WHEN** um usuário consulta ou tenta acessar diretamente um registro associado a veículo de outro prestador
- **THEN** o sistema rejeita o acesso sem revelar os dados do item.

#### Scenario: Identificar e contatar o proprietário
- **WHEN** o prestador ou motorista consulta os dados disponíveis de um item para identificar o proprietário
- **THEN** o sistema fornece o contexto do registro e do frete relacionado, se houver, sem exibir dados pessoais do passageiro nem oferecer comunicação com ele dentro do MUV.

### Requirement: Registrar devolução do objeto
O sistema SHALL permitir que um usuário autorizado confirme a entrega do objeto e atualize sua situação para `DEVOLVIDO`, sem exigir termo assinado.

#### Scenario: Confirmar entrega
- **WHEN** o prestador responsável ou motorista vinculado confirma a entrega de um item em `EM_GUARDA`
- **THEN** o sistema atualiza a situação do item para `DEVOLVIDO`.

#### Scenario: Item ainda não devolvido
- **WHEN** um item não teve sua entrega confirmada
- **THEN** o sistema mantém sua situação como `EM_GUARDA`.

#### Scenario: Compatibilidade com item legado em custódia
- **WHEN** um registro existente estiver com a situação `EM_CUSTODIA`
- **THEN** o sistema o apresenta como item em guarda e permite confirmar sua devolução
- **AND** preserva o registro até a confirmação, atualizando sua situação para `DEVOLVIDO` quando confirmada.
