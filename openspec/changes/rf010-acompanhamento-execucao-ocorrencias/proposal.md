# RF010 - Acompanhamento da execução e registro de ocorrências

## Why

Após a designação do RF009, o MUV precisa acompanhar a execução do frete e distinguir uma ocorrência informativa de uma interrupção, retomada ou encerramento sem conclusão. RF010, US-012 e RN015 exigem operações autorizadas e um histórico que preserve os responsáveis, os horários e as justificativas.

## What Changes

- Permitir ao motorista designado iniciar o frete somente com motorista e veículo devidamente designados.
- Permitir ao prestador responsável ou ao motorista atualmente designado para aquele frete registrar ocorrência sem interromper, interromper a execução, retomar após interrupção ou encerrar sem conclusão.
- Restringir consulta e comandos ao vínculo com o frete; pertencer à mesma empresa não autoriza outro motorista a registrar ocorrências.
- Explicitar o estado anterior e o resultante de cada ação, preservando a identidade do autor autenticado, data, horário, descrição ou justificativa no histórico de execução.
- Usar as três telas fornecidas para registro/interrupção, retomada e encerramento, com efeito da ação visível e retorno sem gravação quando não houver confirmação.
- Identificar os eventos de execução previstos para integração com o RF013 e proteger as operações contra confirmações antigas, reenvios e disputas com RF009 e RF011.

## Capabilities

### New Capabilities

- `acompanhamento-execucao-ocorrencias`: acompanhamento autorizado do frete, início, ocorrências sem interrupção, interrupções, retomadas, encerramentos sem conclusão, histórico de execução e fatos para integração com o RF013.

### Modified Capabilities

Nenhuma. A mudança consome a contratação do RF008 e a designação do RF009 e respeita o cancelamento pré-execução do RF011, sem alterar seus requisitos.

## Impact

Gustavo é o responsável pelo RF010 e Walison participa da revisão. A branch contém consulta operacional, início, ocorrência, interrupção, retomada e encerramento sem conclusão, com interface, API e persistência. O caminho verificado para o acompanhamento é o acesso direto a `/fretes/:freteId/execucao`. A descoberta da tela a partir do fluxo do prestador ou do motorista segue em aberto.

A consulta usa a sessão RF001 e os vínculos e recursos de RF003/RF009. O fluxo completo do autônomo depende da associação da conta ao motorista próprio e da designação em RF002/RF009. O RF010 grava os fatos de execução. A política de avisos e a entrega dependem de Rafael (RF013). A leitura geral atual do RF014 já encontra os fatos, e a evolução do contrato compartilhado depende de Josué. As decisões e as pendências constam no [design](design.md#open-questions).

Esta revisão atualiza os cinco documentos da mudança OpenSpec. O escopo funcional continua limitado ao RF010. Cancelamento pré-execução, conclusão efetiva, substituição de recursos, novo acordo, central de notificações e operações financeiras pertencem aos respectivos módulos.
