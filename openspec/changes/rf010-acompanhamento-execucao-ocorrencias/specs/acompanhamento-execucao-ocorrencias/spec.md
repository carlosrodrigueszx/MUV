# Spec Delta

## Purpose

Permitir o acompanhamento autorizado da execução de fretes de pessoas e encomendas, distinguindo início, ocorrência, interrupção, retomada e encerramento sem conclusão e preservando a autoria e a sequência das alterações.

## ADDED Requirements

### Requirement: Autorizar as ações pelo vínculo atual com o frete

O sistema MUST permitir ao prestador responsável ou ao motorista atualmente designado para aquele frete registrar ocorrência sem interrupção, interromper, retomar e encerrar sem conclusão, respeitando o estado elegível de cada ação. O autor SHALL ser identificado pela sessão autenticada e a confirmação SHALL exigir proteção CSRF.

#### Scenario: Prestador responsável confirma uma ação

- **GIVEN** um frete sob responsabilidade do prestador autenticado, com motorista designado em outra conta e em estado elegível para a ação escolhida
- **WHEN** o prestador confirma uma ocorrência, interrupção, retomada ou encerramento sem conclusão com os dados exigidos
- **THEN** o sistema aceita a ação e registra a própria conta como autora
- **AND** não atribui a ação ao motorista apenas por ele estar designado.

#### Scenario: Motorista designado confirma uma ação

- **GIVEN** a conta autenticada associada ao motorista atualmente designado para o frete, distinta da conta do prestador responsável
- **WHEN** esse motorista confirma uma das quatro ações com estado e dados válidos
- **THEN** o sistema aceita a ação e identifica essa conta como autora
- **AND** mantém o prestador contratado como responsável pelo serviço.

#### Scenario: Outro motorista da mesma empresa tenta registrar ocorrência

- **GIVEN** um motorista autenticado, vinculado e verificado pela mesma empresa responsável, mas não designado para aquele frete
- **WHEN** ele tenta registrar uma ocorrência, inclusive enviando a confirmação diretamente
- **THEN** o sistema rejeita o acesso sem alterar situação, histórico ou notificações
- **AND** não o autoriza apenas pelo vínculo com a empresa.

#### Scenario: Motorista substituído tenta confirmar uma tela antiga

- **GIVEN** uma tela aberta pelo motorista que foi substituído pelo RF009
- **WHEN** ele confirma uma ação após a troca de designação
- **THEN** o sistema reavalia a designação atual e rejeita a ação desse motorista sem gravações.

#### Scenario: Sessão ausente, terceiro ou confirmação sem CSRF

- **WHEN** uma conta de outro prestador, um cliente sem papel de executor autorizado, uma sessão ausente ou uma confirmação sem proteção CSRF válida tenta executar uma ação
- **THEN** o sistema rejeita a operação sem expor detalhes protegidos nem produzir alterações.

#### Scenario: Tentativa de falsificar o autor

- **WHEN** um usuário envia identificador de autor ou papel diferente daquele autorizado pela sua sessão
- **THEN** o sistema não utiliza esses dados para conceder permissão nem atribuir autoria
- **AND** uma ação aceita permanece atribuída ao usuário autenticado que efetivamente confirmou. Se o servidor recusar o comando por falta de autorização, a situação e o histórico permanecem intactos, sem efeito de integração.

### Requirement: Consultar o acompanhamento do próprio frete

O sistema SHALL permitir ao prestador responsável e ao motorista atualmente designado consultar situação, recursos vigentes, histórico e ações elegíveis do frete. O gestor SHALL consultar somente fretes sob responsabilidade da sua organização. A consulta MUST respeitar a privacidade e não produzir eventos de execução.

#### Scenario: Gestor consulta o frete da organização

- **WHEN** o prestador responsável consulta o acompanhamento de um frete de sua organização
- **THEN** o sistema apresenta o identificador real do frete, situação exata, motorista e veículo atuais e histórico autorizado
- **AND** distingue as ações disponíveis em `EM_EXECUCAO` das disponíveis em `INTERROMPIDO`.

#### Scenario: Motorista consulta o frete designado

- **WHEN** o motorista atualmente designado abre o acompanhamento do frete
- **THEN** o sistema apresenta o contexto e as ações permitidas à sua conta para aquele frete.

#### Scenario: Acesso ao acompanhamento de outro frete

- **WHEN** um gestor consulta frete de outra organização ou um motorista consulta frete para o qual não está designado
- **THEN** o sistema rejeita a consulta e não revela ocorrências, justificativas ou dados pessoais protegidos.

#### Scenario: Abrir o acompanhamento pela rota do frete

- **GIVEN** uma sessão do prestador responsável ou do motorista designado e o identificador real do frete
- **WHEN** a conta abre `/fretes/:freteId/execucao`
- **THEN** o sistema consulta o contexto do frete e apresenta as ações elegíveis para a sessão.

### Requirement: Registrar o início somente após designação válida

O sistema SHALL permitir ao motorista atualmente designado iniciar um frete em `PRONTO_INICIO` com motorista e veículo devidamente designados e elegíveis conforme RF009. A confirmação MUST mudar a situação para `EM_EXECUCAO` e acrescentar um evento `INICIO` com autor, data, horário e estados anterior e resultante.

#### Scenario: Motorista inicia o frete pronto

- **GIVEN** um frete em `PRONTO_INICIO` com designação válida
- **WHEN** o motorista designado confirma o início
- **THEN** o estado anterior é `PRONTO_INICIO` e o resultante é `EM_EXECUCAO`
- **AND** o histórico registra `INICIO` vinculado ao frete e à conta desse motorista.

#### Scenario: Início sem recursos ou em estado incompatível

- **WHEN** há tentativa de início em `AGUARDANDO_DESIGNACAO`, com motorista ou veículo ausente ou inelegível, ou em qualquer estado diferente de `PRONTO_INICIO`
- **THEN** o sistema rejeita a operação e preserva estado, histórico e notificações.

### Requirement: Registrar ocorrência sem interromper a execução

O sistema SHALL permitir ao prestador responsável ou ao motorista designado registrar uma ocorrência em `EM_EXECUCAO`, exigindo descrição, data e horário. A ação MUST acrescentar `OCORRENCIA` ao histórico e manter o frete em `EM_EXECUCAO`, sem abrir interrupção nem registrar conclusão.

#### Scenario: Registrar um atraso sem parar o serviço

- **GIVEN** um frete em `EM_EXECUCAO`
- **WHEN** um dos dois responsáveis autorizados confirma uma ocorrência com descrição, data e horário válidos
- **THEN** o estado anterior e o resultante são `EM_EXECUCAO`
- **AND** o histórico identifica tipo `OCORRENCIA`, descrição, data, horário, autor e seu papel nessa ação.

#### Scenario: Ocorrência sem descrição ou fora da execução ativa

- **WHEN** o usuário confirma uma ocorrência sem descrição com conteúdo ou em um frete que não esteja em `EM_EXECUCAO`
- **THEN** o sistema rejeita o registro e mantém a situação e os registros anteriores.

### Requirement: Interromper a execução mediante justificativa

O sistema SHALL permitir ao prestador responsável ou ao motorista designado interromper um frete em `EM_EXECUCAO`, com justificativa, data e horário obrigatórios. A confirmação MUST mudar a situação para `INTERROMPIDO` e acrescentar `INTERRUPCAO` ao histórico sem concluir ou cancelar o frete.

#### Scenario: Confirmar a interrupção

- **GIVEN** um frete em `EM_EXECUCAO`
- **WHEN** um responsável autorizado confirma a interrupção com justificativa, data e horário válidos
- **THEN** o estado anterior é `EM_EXECUCAO` e o resultante é `INTERROMPIDO`
- **AND** o histórico registra `INTERRUPCAO`, motivo, autor e seu papel, data e horário
- **AND** motorista e veículo continuam designados.

#### Scenario: Interrupção sem justificativa

- **WHEN** um autor autorizado tenta interromper em `EM_EXECUCAO` sem justificativa com conteúdo
- **THEN** o sistema rejeita a interrupção e preserva o estado e o histórico.

#### Scenario: Nova tentativa de interromper um frete já interrompido

- **GIVEN** um frete em `INTERROMPIDO`
- **WHEN** um autor autorizado confirma uma nova operação de interrupção, distinta da confirmação anterior
- **THEN** o sistema recusa a operação e preserva `INTERROMPIDO` e o histórico
- **AND** não produz efeito de integração.

### Requirement: Retomar um frete interrompido

O sistema SHALL permitir ao prestador responsável ou ao motorista designado retomar um frete em `INTERROMPIDO`, exigindo justificativa, data, horário, recursos válidos conforme RF009 e confirmação de que é possível prosseguir. A confirmação MUST produzir `EM_EXECUCAO` e um evento `RETOMADA`, preservando a interrupção anterior.

#### Scenario: Confirmar a retomada da interrupção em aberto

- **GIVEN** um frete em `INTERROMPIDO` com motorista e veículo válidos
- **WHEN** um responsável autorizado informa justificativa, data e horário e confirma que é possível prosseguir
- **THEN** o estado anterior é `INTERROMPIDO` e o resultante é `EM_EXECUCAO`
- **AND** o histórico registra `RETOMADA` com autor e seu papel, justificativa, data e horário, vinculada à interrupção em aberto
- **AND** conserva o registro original da interrupção.

#### Scenario: Retomada após substituição de recursos

- **GIVEN** novos recursos designados pelo RF009 durante a interrupção e o frete ainda em `INTERROMPIDO`
- **WHEN** o prestador responsável ou o novo motorista designado confirma a retomada com contexto atualizado e recursos válidos
- **THEN** o sistema passa para `EM_EXECUCAO` usando a designação vigente
- **AND** identifica o verdadeiro autor da retomada, preservando os autores dos eventos anteriores.

#### Scenario: Retomada sem condições de prosseguir

- **WHEN** falta justificativa, data, horário ou confirmação explícita, os recursos são inválidos ou o frete já não está `INTERROMPIDO`
- **THEN** o sistema rejeita a retomada e preserva a situação atual, sem evento ou notificação novos.

### Requirement: Encerrar sem registrar conclusão

O sistema SHALL permitir ao prestador responsável ou ao motorista designado encerrar um frete em `INTERROMPIDO`, exigindo justificativa, data, horário e confirmação de que o serviço não foi concluído. A confirmação MUST produzir `ENCERRADO_SEM_CONCLUSAO` e `ENCERRAMENTO_SEM_CONCLUSAO`, sem registrar entrega, conclusão ou cancelamento.

#### Scenario: Confirmar impossibilidade de retomar

- **GIVEN** um frete em `INTERROMPIDO`
- **WHEN** um responsável autorizado informa os dados obrigatórios e confirma que o frete não foi concluído
- **THEN** o estado anterior é `INTERROMPIDO` e o resultante é `ENCERRADO_SEM_CONCLUSAO`
- **AND** o histórico registra `ENCERRAMENTO_SEM_CONCLUSAO`, autor e seu papel, justificativa, data e horário, vinculados à interrupção em aberto
- **AND** não preenche dados de conclusão ou recebimento da encomenda, não registra `CONCLUSAO` ou `CANCELAMENTO` e preserva valor e condições contratadas.

#### Scenario: Encerramento sem motivo ou confirmação

- **WHEN** falta justificativa com conteúdo, data, horário ou confirmação de que o frete não foi concluído
- **THEN** o sistema não encerra o frete e mantém `INTERROMPIDO`, sem acrescentar registros.

#### Scenario: Tentativa de encerrar diretamente uma execução ativa

- **WHEN** um responsável tenta encerrar sem conclusão um frete em `EM_EXECUCAO`
- **THEN** o sistema rejeita a operação e informa que é necessário registrar a interrupção antes do encerramento.

### Requirement: Preservar o histórico e a autoria de cada ação

O sistema MUST manter um histórico cronológico e imutável com identificador do evento e do frete, tipo, estados anterior e resultante, conta autora, data e horário e descrição ou justificativa exigida. A apresentação do papel histórico segue a decisão P1 no [design](../../design.md#open-questions). A identificação do autor MUST corresponder a quem confirmou, independentemente do prestador ou motorista atualmente responsável.

#### Scenario: Consultar uma sequência de ações de autores diferentes

- **WHEN** um usuário autorizado consulta início, ocorrência, interrupção, retomada e encerramento registrados por contas diferentes
- **THEN** o acompanhamento apresenta o autor e o horário de cada evento, e o recibo de confirmação preserva os estados da ação
- **AND** eventos anteriores não são reatribuídos a quem executou a última ação nem ao motorista atual.

#### Scenario: Registrar novo evento preservando os anteriores

- **WHEN** uma nova ação autorizada é confirmada
- **THEN** o sistema acrescenta um evento sem editar ou excluir ocorrências, interrupções ou justificativas anteriores
- **AND** uma retomada ou encerramento permite identificar a interrupção à qual se refere.

### Requirement: Validar os dados temporais e os textos obrigatórios

O sistema SHALL rejeitar textos obrigatórios ausentes, vazios ou compostos somente por espaços, inclusive Unicode, e datas ou horários ausentes ou com formato ou valor de calendário inválido, quando exigidos no formulário. O sistema SHALL guardar a data e hora civis informadas nos formulários separadamente do instante de auditoria em UTC e exibi-las no RF010 sem conversão de fuso.

#### Scenario: Dados obrigatórios inválidos

- **WHEN** o usuário tenta confirmar uma ação com descrição ou justificativa obrigatória sem conteúdo, data inexistente ou horário inválido ou ausente
- **THEN** o sistema informa os campos a corrigir e não muda a situação nem acrescenta eventos ou notificações.

#### Scenario: Preencher os dados antes da confirmação

- **WHEN** o usuário informa data e horário válidos no formulário
- **THEN** o preenchimento não altera o estado do frete nem grava um evento sem confirmação.

#### Scenario: Consultar horário informado e campos operacionais opcionais

- **WHEN** uma conta autorizada consulta uma ocorrência ou interrupção com local informado ou um encerramento de encomenda com situação informada
- **THEN** o acompanhamento exibe o horário civil informado e o respectivo campo opcional
- **AND** o local e a situação da encomenda ficam restritos à consulta operacional do RF010.

### Requirement: Confirmar o efeito da ação e permitir voltar sem registrar

A interface SHALL apresentar o frete, a ação selecionada, os campos exigidos e seu efeito sobre a situação antes da confirmação. O sistema MUST efetivar a ação somente após confirmação explícita. Voltar, cancelar o formulário ou sair antes de confirmar MUST preservar o estado e não criar evento ou notificação.

#### Scenario: Rever o efeito da interrupção

- **WHEN** o usuário escolhe interrupção no formulário de um frete em `EM_EXECUCAO`
- **THEN** a interface informa que a confirmação mudará a situação para `INTERROMPIDO`, preservará autor e motivo e exibirá o aviso de notificação previsto no protótipo, conforme P4 no [design](../../design.md#open-questions)
- **AND** a simples seleção não grava a ocorrência nem interrompe o frete.

#### Scenario: Voltar sem registrar ocorrência ou interrupção

- **GIVEN** um formulário preenchido para ocorrência ou interrupção em `EM_EXECUCAO`
- **WHEN** o usuário aciona “Voltar sem registrar” ou sai sem confirmar
- **THEN** o frete permanece `EM_EXECUCAO` e nenhum evento ou notificação é criado por esse formulário.

#### Scenario: Voltar sem confirmar a retomada

- **GIVEN** um formulário de retomada preenchido e um frete em `INTERROMPIDO`
- **WHEN** o usuário aciona “Voltar ao acompanhamento” sem confirmar
- **THEN** o frete permanece `INTERROMPIDO` e a interrupção continua em aberto, sem novo evento ou notificação.

#### Scenario: Voltar e manter interrompido

- **GIVEN** um formulário de encerramento preenchido e um frete em `INTERROMPIDO`
- **WHEN** o usuário aciona “Voltar e manter interrompido” sem confirmar
- **THEN** o frete permanece `INTERROMPIDO`, sem evento de encerramento e sem notificação.

### Requirement: Registrar os fatos de execução nas confirmações aceitas

O RF010 SHALL gravar o evento e a situação resultante em uma única operação. Os eventos de interrupção, retomada e encerramento sem conclusão são os fatos para os quais as telas preveem aviso. A entrega da integração segue P4 no [design](../../design.md#open-questions).

#### Scenario: Interrupção registrada com sucesso

- **WHEN** um responsável autorizado confirma a interrupção
- **THEN** o RF010 grava o evento de interrupção com a autoria da sessão e o novo estado do frete
- **AND** o evento confirmado pode ser identificado pelo contrato de integração a alinhar com o RF013.

#### Scenario: Falha ao persistir o fato de execução

- **WHEN** não é possível gravar a situação ou o evento
- **THEN** nenhuma das gravações da ação permanece e não há fato confirmado para integração.

### Requirement: Revalidar ações concorrentes e confirmações repetidas

O sistema MUST revalidar autorização, estado, designação e contexto do ciclo de execução ou da interrupção ao confirmar. Novas operações com contexto desatualizado MUST ser rejeitadas. O reenvio autorizado da mesma confirmação já processada SHALL recuperar seu resultado original sem duplicar o evento nem repetir efeitos de integração.

#### Scenario: Início disputa com cancelamento do RF011

- **WHEN** um início e um cancelamento concorrentes disputam um frete em `PRONTO_INICIO`
- **THEN** somente a primeira transição válida é aplicada após reavaliar o estado vigente
- **AND** `CANCELADO` impede o início e `EM_EXECUCAO` impede o cancelamento, sem retorno a estado pré-execução.

#### Scenario: Retomada disputa com encerramento

- **WHEN** retomada e encerramento disputam a mesma interrupção em aberto
- **THEN** somente uma transição baseada nessa interrupção é aplicada e a outra confirmação recebe conflito
- **AND** o sistema preserva a ação vencedora e seu autor.

#### Scenario: Interrupção foi substituída por outro ciclo

- **GIVEN** um formulário referente a uma interrupção já retomada e uma nova interrupção posterior
- **WHEN** o usuário confirma o formulário antigo mesmo encontrando novamente o estado `INTERROMPIDO`
- **THEN** o sistema rejeita o contexto antigo e exige atualização, sem aplicar a ação à nova interrupção.

#### Scenario: Formulário de ocorrência ou interrupção de um ciclo anterior

- **GIVEN** um formulário de ocorrência ou interrupção aberto em `EM_EXECUCAO`
- **AND** enquanto o formulário estava aberto o frete foi interrompido e retomado com a mesma designação
- **WHEN** o usuário confirma uma nova operação pelo formulário antigo
- **THEN** o sistema pede atualização, mesmo com o frete novamente em `EM_EXECUCAO`
- **AND** preserva o estado e o histórico atuais, sem efeito de integração.

#### Scenario: Reenvio autorizado da mesma confirmação após perda da resposta

- **GIVEN** uma confirmação já processada cuja resposta se perdeu, por exemplo após uma queda de conexão
- **WHEN** a mesma conta, ainda autorizada para aquele frete, reenvia a mesma confirmação com a mesma identidade de operação e conteúdo
- **THEN** o sistema devolve o resultado original mesmo que a situação já tenha mudado, inclusive se a primeira interrupção já deixou o frete em `INTERROMPIDO`
- **AND** não cria outro evento, não altera o estado vigente nem repete efeitos de integração.

### Requirement: Respeitar os limites do ciclo de vida do frete

O sistema MUST impedir comandos RF010 em `CANCELADO`, `CONCLUIDO` e `ENCERRADO_SEM_CONCLUSAO`. Interrupção e encerramento sem conclusão MUST preservar a distinção em relação ao cancelamento pré-execução do RF011 e à conclusão efetiva do RF012, sem alterar automaticamente valores ou condições financeiras.

#### Scenario: Tentar alterar um frete terminal

- **WHEN** um usuário tenta iniciar, registrar ocorrência, interromper, retomar ou encerrar um frete em estado terminal
- **THEN** o sistema rejeita o novo comando e conserva situação, histórico e notificações anteriores.

#### Scenario: Tentar cancelar um frete interrompido

- **WHEN** o usuário solicita cancelamento pré-execução para um frete em `INTERROMPIDO`
- **THEN** o cancelamento é impedido conforme RF011
- **AND** as opções do RF010 permanecem retomada ou encerramento sem conclusão, conforme as permissões e validações aplicáveis.
