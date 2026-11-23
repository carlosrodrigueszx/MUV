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

Gustavo é o responsável pelo RF010. Gustavo e Walison revisam a proposta antes de iniciar a implementação.

O trabalho futuro envolve o portal, a API e a persistência dos fatos de execução. O RF010 usa a sessão do RF001 e consulta vínculo e designação em RF003/RF009. Cadastro, designação e substituição continuam nos módulos responsáveis. O autônomo depende de RF002/RF009 para que a conta seja associada ao motorista próprio e o frete chegue a `PRONTO_INICIO`.

O RF010 registra seus próprios eventos. Rafael responde pela integração e política de avisos do RF013. Josué responde pela consulta geral, filtros e apresentação do RF014. Alterações em contratos, tabelas ou comportamentos compartilhados dependem de alinhamento. As decisões pendentes e os cenários afetados estão em [design.md — Open Questions](design.md#open-questions).

Esta entrega contém somente os artefatos OpenSpec, incluindo `.openspec.yaml`. Não implementa frontend, backend, banco ou testes da aplicação. A implementação futura depende de autorização conforme `AGENTS.md`; cancelamento, conclusão efetiva/entrega (RF012), substituição de recursos e novo acordo, central de notificações e operações financeiras permanecem nos respectivos módulos.
