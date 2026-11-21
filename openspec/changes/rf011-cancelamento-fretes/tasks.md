# Tarefas do RF011

## 1. Integração e especificação

- [x] Conferir situação do Git, worktrees e PRs, atualizar origin, criar backup e iniciar merge local.
- [x] Validar a mudança ativa com `openspec validate rf011-cancelamento-fretes --strict`.

## 2. Código e persistência

- [x] Preservar as rotas da main e de cancelamento.
- [x] Ligar o RF008 ao cancelamento pelo ID real com ação limitada aos estados elegíveis.
- [x] Conferir contrato do RF014 e validar situação e evento de cancelamento.
- [x] Reutilizar as migrations integradas e adaptar o teste de geração de IDs.
- [x] Verificar concorrência com RF009 e contrato SQL de início do RF010.

## 3. Validação

- [x] Executar build, lint e checagem do diff.
- [x] Executar testes de serviço, API e Flyway em PostgreSQL dedicado.
- [x] Executar o fluxo RF008 → RF011 → RF014 no navegador.
