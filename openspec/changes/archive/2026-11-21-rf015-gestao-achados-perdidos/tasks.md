# Tasks

## 1. Verificações antes da implementação

- [x] 1.1 Conferir branch, `git status`, commits recentes, worktrees, PRs pendentes e migrações Flyway antes de editar; verificar que arquivos compartilhados ou protegidos não serão sobrescritos. O PR #22 altera `Router.tsx`; a edição aditiva das rotas do RF015 foi autorizada pelo usuário.
- [x] 1.2 Verificar o DDL atual de `Achado_Perdido` (status sem enum/check e IDs sem default), vínculos de veículos/motoristas e persistência de imagens; registrar a decisão no design e reutilizar o padrão existente de foto binária, sem serviço ou dependência externa.

## 2. Persistência e API

- [x] 2.1 Criar migração aditiva para sequências de IDs, armazenamento de imagem compatível com URL legada e vínculo opcional entre achado e frete; validar a associação ao mesmo prestador e comprovar em PostgreSQL limpo e atualizado que registros existentes são preservados.
- [x] 2.2 Implementar o registro do item com foto, descrição, data, veículo e frete opcional, iniciando em `EM_GUARDA`; verificar campos obrigatórios, relação da foto e persistência no teste de integração.
- [x] 2.3 Implementar a consulta dos registros e a confirmação da entrega, aplicando autorização pelo prestador responsável ou motorista vinculado em todas as operações; verificar acesso próprio, acesso indevido, frete opcional e transição para `DEVOLVIDO` em testes de API/serviço.
- [x] 2.4 Cobrir atomicidade, validação e isolamento entre prestadores com testes automatizados compatíveis com a suíte existente; executar os testes do RF015 e registrar o resultado real.

## 3. Portais do prestador e do motorista

- [x] 3.1 Implementar a consulta dos itens no portal do motorista, mostrando os dados previstos e ações disponíveis conforme a situação; verificar carregamento, lista vazia, erro e itens em guarda/devolvidos com respostas da API. Portal usa a tela compartilhada com o prestador; integração cobre listagem por motorista vinculado e estados em guarda/devolvido.
- [x] 3.2 Implementar o formulário de registro para prestador e motorista autorizado, com foto, descrição, data, veículo e frete opcional; verificar que o envio cria o item em guarda e que dados de exemplo dos protótipos não são usados como dados reais.
- [x] 3.3 Implementar a confirmação de entrega e atualizar a situação apresentada após resposta da API; verificar que o item passa a devolvido sem solicitar termo assinado nem dados pessoais do proprietário.
- [x] 3.4 Validar responsividade, navegação e estados da interface nos fluxos implementados; executar o build e os testes de frontend relacionados e registrar os resultados. Build e ESLint passaram; não há testes frontend dedicados no repositório.

## 4. Integração e validação do fluxo

- [x] 4.1 Executar o fluxo de sistema de registrar, consultar e confirmar devolução usando frontend, backend e PostgreSQL; verificar persistência, autorização por prestador e resultado visível nos portais. Fluxo ponta a ponta do prestador confirmado no navegador; testes de integração confirmam o acesso de motorista vinculado e isolamento por prestador.
- [x] 4.2 Executar a suíte pertinente e o build do projeto após a integração; revisar regressões, console e logs e registrar comandos e resultados reais antes de concluir a mudança. Testes backend RF015: 14 aprovados; build frontend e ESLint aprovados; validação OpenSpec aprovada; console sem exceções da aplicação.
