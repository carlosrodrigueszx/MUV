# RF007 — Apresentação de propostas

## Why

O prestador precisa responder à oferta de frete do cliente com um valor e condições que possam ser avaliados antes da contratação. A US-007 e a RN009 exigem esse fluxo para pessoas e encomendas.

## What Changes

- Mostrar ao prestador o trajeto pelas vias no mapa e, em “O caminho planejado”, a partida e a chegada prevista em cada parada e no destino.
- Avaliar a solicitação no portal do prestador e responder pelo valor da proposta do cliente ou enviar contraproposta justificada.
- Validar autorização por modalidade, situação da solicitação e destinatário, registrar a resposta e notificar o cliente.
- Cobrir as regras com testes unitários e o fluxo completo do prestador com Selenium no ambiente de testes do backend.

## Capabilities

### New Capabilities

- `apresentacao-de-propostas` cobre a avaliação e o envio da oferta do prestador até o registro e a notificação.

### Modified Capabilities

## Impact

A mudança envolve React, API Spring Boot e persistência PostgreSQL. Consome os dados da solicitação e entrega propostas para o RF008. A consulta geral de solicitações, as telas do cliente, contratação e pagamento ficam fora do escopo. Referências normativas: documento da branch documents, US-007 (pp. 8–9), RN009 (p. 17), RF007 (pp. 19 e 31), e frames locais avaliar-itinerario e enviar-contraproposta.
