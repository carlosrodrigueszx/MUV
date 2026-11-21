# Tasks

## 1. Pré-requisitos e persistência

- [x] 1.1 Registrar a autorização explícita do backend RF008 antes de alterar código e revisar os PRs relevantes para router, migrations e cancelamento. Registrar a coordenação necessária com o RF011.
- [x] 1.2 Configurar a geração de ID de frete por migration incremental se o esquema integrado ainda não a oferecer. Validar em PostgreSQL com fretes preexistentes e verificar que as duas restrições únicas da V1 permanecem efetivas.

## 2. API de cliente e confirmação

- [x] 2.1 Implementar as consultas de propostas e de frete com filtro de titularidade e DTOs de dados públicos. Cobrir com MockMvc a falta de sessão, o acesso de outro cliente, a ausência de propostas e as duas modalidades.
- [x] 2.2 Implementar o aceite transacional, a repetição segura e a emissão da notificação ao prestador na mesma transação. Cobrir a persistência dos valores e estados, reenvio, conflito, proposta inelegível, CSRF e rollback por falha da notificação.
- [x] 2.3 Exercitar em PostgreSQL isolado dois aceites simultâneos, a corrida com o envio do RF007 e, quando integrado, o cancelamento que encerra uma solicitação. Verificar que a ordem de confirmação preserva a elegibilidade e que existe no máximo um frete e uma notificação de contratação.

## 3. Interface do cliente

- [x] 3.1 Conferir os frames de comparação, confirmação e detalhe com `get_design_context` e `get_screenshot` quando o Figma MCP estiver disponível. Registrar as referências verificadas e confirmar o ponto de entrada antes das alterações visuais.
- [x] 3.2 Integrar comparação, ação de aceite e detalhe contratado às rotas e à API acordadas. Verificar navegação por ID, recarga do detalhe, estados vazio, carregando e erro, conflito de aceite e aderência aos frames em resoluções suportadas.

## 4. Validação integrada

- [x] 4.1 Executar o fluxo Selenium com dados controlados, da consulta até o detalhe, confirmando a notificação do prestador no ambiente de testes. Exercitar ambas as modalidades e verificar logs e console.
- [x] 4.2 Executar `./mvnw test` em `backend/` no perfil de teste com PostgreSQL isolado e `npm run build` em `frontend/`. Registrar os resultados e verificar regressões pertinentes do RF007 e do histórico.

## Resultado da validação

- Build do frontend e lint dos arquivos alterados concluídos com sucesso.
- Em PostgreSQL 16 isolado, a migration preservou os fretes existentes e as duas constraints únicas. Os 13 testes direcionados de RF008, RF007 e histórico passaram, incluindo dois fluxos Selenium.
- A suíte Maven completa foi executada. As falhas de seleção na automação RF008 e de origem HTTP no teste RF004 foram resolvidas nas reexecuções direcionadas. O teste de responsividade do mapa `PercursoSystemTest.paradasOrdenadas` também falhou na mesma asserção em uma cópia de `b64aba5`, anterior ao RF008.
- O lint global reportou duas ocorrências preexistentes de `react-refresh/only-export-components` no índice de vinculação de motoristas.
