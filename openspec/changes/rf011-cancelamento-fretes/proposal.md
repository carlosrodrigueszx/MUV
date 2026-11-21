# RF011 — Cancelamento de Fretes

## Why

Cliente e prestador responsável precisam cancelar um frete contratado antes da execução, com rastreabilidade e aviso à outra parte, conforme RF011, RN014 e RU005.

## What Changes

- Integrar a confirmação de cancelamento à consulta de frete contratado.
- Exigir vínculo, estado elegível e justificativa, registrando situação, evento e notificação na mesma transação.
- Manter a consulta do resultado no histórico. Central de notificações, início da execução e operações financeiras ficam fora do escopo.

## Capabilities

### New Capabilities

- `cancelamento-de-fretes` cobre autorização, confirmação, transição, auditoria e notificação interna do cancelamento.

## Impact

React e Spring Boot usam a sessão do RF001, o frete criado pelo RF008, os defaults das migrations vigentes e o histórico do RF014. A coordenação de estados envolve RF009 e RF010. O registro interno usa a tabela de notificações destinada ao RF013.
