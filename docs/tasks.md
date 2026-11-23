# Tasks

## 1. Preparação

- [ ] 1.1 Confirmar a autorização de backend do RF012 e registrar o contrato com RF010, incluindo o bloqueio compartilhado e os estados elegíveis.
- [x] 1.2 Conferir os PNGs e JSONs exportados para pessoas e encomendas e registrar os campos e a estrutura das duas telas em `design.md`.
- [ ] 1.3 Consolidar o contrato de leitura, a exposição do CPF e as convenções de data/hora em `design.md`, incluindo o efeito sobre o filtro mensal do RF014.
- [ ] 1.4 Confirmar o ponto de entrada no acompanhamento, a consulta pelo cliente e os estados visuais ainda sem referência.

## 2. Registro e consulta

- [ ] 2.1 Implementar a transição e a autorização do motorista designado com sessão, CSRF e atualização condicional. Cobrir ausência de sessão, acesso de terceiros, motorista substituído e estados inelegíveis com testes unitários e MockMvc.
- [ ] 2.2 Implementar a validação por modalidade e a persistência dos dados de conclusão, com testes de sucesso para pessoas e encomendas e falha para dados ausentes ou inválidos.
- [ ] 2.3 Integrar o evento de histórico e a notificação interna na transação. Testar em PostgreSQL dois envios concorrentes e rollback quando a notificação falhar.
- [ ] 2.4 Disponibilizar a leitura autorizada e verificar os detalhes do cliente contratante, a proteção dos dados do recebedor e a apresentação no RF014.

## 3. Interface

- [ ] 3.1 Implementar o ponto de entrada autorizado e o formulário responsivo conforme os frames, com tratamento das duas modalidades e feedback de sucesso e erro. Verificar no navegador a data e o horário informados nas duas modalidades, nome e CPF somente em encomendas, observações opcionais, confirmação explícita e os estados previstos.
- [ ] 3.2 Integrar a consulta da conclusão e a elegibilidade para contestação conforme a US-015. Verificar a atualização dos dados e a navegação do cliente.

## 4. Validação integrada

- [ ] 4.1 Executar build e lint do frontend e a suíte pertinente do backend. Registrar os resultados e eventuais limitações.
- [ ] 4.2 Executar os fluxos integrados em Selenium com dados controlados, cobrindo as duas modalidades, acesso indevido, estado inválido e efeitos no histórico e nas notificações. Conferir console e logs.
