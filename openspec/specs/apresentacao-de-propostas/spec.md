# apresentacao-de-propostas Specification

## Purpose

Permitir que prestadores autorizados avaliem uma solicitação e ofereçam o serviço pelo valor proposto pelo cliente ou por outro valor justificado.

## Requirements

### Requirement: Avaliação da solicitação
O portal SHALL consultar a API para a solicitação selecionada e apresentar os dados retornados para sua modalidade, suas condições disponíveis e seu valor oferecido com a identificação “Proposta do cliente”.

#### Scenario: Avaliar transporte de pessoas
- **WHEN** o prestador abre uma solicitação de pessoas com itinerário, paradas, horários e dados do grupo
- **THEN** a tela apresenta o trajeto, os dados do grupo, as condições do serviço e o valor em reais conforme o frame Avaliar itinerário.

#### Scenario: Oferta indisponível
- **WHEN** a API falha ou a solicitação consultada não possui valor da oferta do cliente
- **THEN** a interface informa a indisponibilidade e impede o aceite pelo valor do cliente, inclusive após o prestador ter consultado outra solicitação com oferta disponível.

#### Scenario: Avaliar transporte de encomendas
- **WHEN** o prestador abre uma solicitação de encomenda
- **THEN** a avaliação apresenta volumes, coleta e entrega pertinentes ao serviço e permite responder à proposta do cliente.

### Requirement: Mapa do percurso e horários por ponto
O portal SHALL apresentar o trajeto da solicitação em mapa geográfico e mostrar, em “O caminho planejado”, a partida e a chegada prevista em cada parada e no destino.

#### Scenario: Visualizar o trajeto pelas vias
- **WHEN** o prestador abre uma avaliação com rota disponível
- **THEN** o mapa enquadra todo o traçado viário, da origem A ao destino B, passando pelas paradas numeradas na ordem da solicitação.
- **AND** em um trajeto direto aparecem a origem e o destino ligados pelo percurso pelas vias.

#### Scenario: Consultar o horário de cada ponto
- **WHEN** a solicitação possui partida e um itinerário com horários planejados ou durações disponíveis por trecho
- **THEN** o cartão apresenta cada horário à esquerda e o endereço correspondente à direita, com marcadores ligados verticalmente até o último ponto.
- **AND** exibe a partida na origem, preserva os horários planejados e estima as chegadas restantes somando as durações dos trechos desde o último horário conhecido, inclusive para o destino.

#### Scenario: Localização ou previsão indisponível
- **WHEN** um endereço não pode ser localizado ou faltam dados para traçar o percurso ou estimar uma chegada
- **THEN** a avaliação identifica a informação indisponível, conserva a ordem, os endereços e os horários conhecidos e mantém as ações de resposta disponíveis conforme a elegibilidade.

### Requirement: Oferecer o serviço pelo valor do cliente
O sistema SHALL permitir ao prestador responder pelo valor apresentado, registrando o valor e as condições revisadas, sem exigir justificativa para manter o preço.

#### Scenario: Responder pelo valor retornado pela API
- **WHEN** o prestador autorizado abre uma solicitação elegível e confirma o valor que a API retornou para ela
- **THEN** o botão de aceite apresenta o valor da solicitação em reais e sua proposta é registrada pelo mesmo valor, vinculada à solicitação e ao prestador, com as condições apresentadas, e o prestador recebe confirmação de envio.
- **AND** a solicitação continua aberta para a decisão do cliente, sem contratação ou pagamento.

### Requirement: Oferecer novo valor com justificativa
O sistema SHALL permitir uma contraproposta com valor monetário positivo e justificativa obrigatória de até 500 caracteres, preservando o itinerário e disponibilizando ao cliente as condições e a mensagem enviada.

#### Scenario: Ajustar valor e enviar
- **WHEN** o prestador abre “Fazer contraproposta”, informa um novo valor e preenche uma justificativa válida
- **THEN** o modal compara o valor digitado com o valor original da solicitação retornado pela API, exibindo a diferença em reais e, para uma base positiva, a variação percentual (diferença dividida pelo valor original, multiplicada por 100).
- **AND** após o envio, a proposta registrada contém o valor digitado pelo prestador e a mensagem informada.

#### Scenario: Impedir dados inválidos
- **WHEN** o valor não é positivo, está ausente ou a justificativa está vazia, contém somente espaços ou excede 500 caracteres
- **THEN** o envio é rejeitado, o motivo é informado e nenhuma proposta ou notificação é criada.

#### Scenario: Voltar à avaliação
- **WHEN** o prestador fecha o modal ou seleciona “Voltar à avaliação” antes do envio
- **THEN** retorna à avaliação da mesma solicitação sem registrar resposta.

### Requirement: Verificar elegibilidade no envio
O sistema SHALL aceitar respostas apenas de prestador autenticado autorizado na modalidade, enquanto a solicitação puder receber propostas, respeitando o destinatário nas solicitações direcionadas.

#### Scenario: Concorrência entre prestadores
- **WHEN** dois prestadores autorizados respondem a uma mesma solicitação pública ainda aberta
- **THEN** as duas propostas permanecem vinculadas aos respectivos autores.

#### Scenario: Solicitação direcionada
- **WHEN** o prestador escolhido responde a uma solicitação direcionada ainda aberta e possui autorização para a modalidade
- **THEN** o sistema aceita a proposta desse prestador.

#### Scenario: Envio inelegível
- **WHEN** a API recebe um envio sem autenticação, sem autorização na modalidade, dirigido a outro prestador ou depois do encerramento da solicitação
- **THEN** rejeita a operação sem registrar proposta, gerar frete ou notificar o cliente, inclusive se a tela estava aberta antes do encerramento.

### Requirement: Recusar com justificativa e respeitar o cancelamento
O portal SHALL permitir a recusa justificada enquanto a solicitação estiver aberta, sem prazo de resposta por relógio.

#### Scenario: Recusa registrada
- **WHEN** o prestador recusa informando o motivo
- **THEN** a avaliação mostra a resposta recusada e a justificativa do prestador.

#### Scenario: Cliente cancela enquanto a avaliação está aberta
- **WHEN** o cliente cancela antes da confirmação do envio ou da recusa
- **THEN** o sistema rejeita a operação e informa que a solicitação está encerrada.

### Requirement: Notificar sobre nova proposta
O sistema SHALL registrar uma notificação de nova proposta para o cliente responsável pela solicitação em cada envio concluído.

#### Scenario: Destinatário e consistência
- **WHEN** uma resposta é confirmada
- **THEN** a proposta e a notificação ao cliente da solicitação ficam disponíveis de forma consistente e o prestador vê o resultado do envio.
