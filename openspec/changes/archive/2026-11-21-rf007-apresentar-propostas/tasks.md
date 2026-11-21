# Tarefas do RF007

## 1. Contratos e proteção dos arquivos compartilhados

- [x] 1.1 Conferir branch, status, worktrees, commits e PRs que atinjam rotas, sessão ou migrações antes de editar. Verificar se a integração proposta preserva alterações de outros integrantes.
- [x] 1.2 Alinhar com o responsável pela solicitação a leitura do valor e das condições do cliente. Verificar um exemplo persistido para pessoas e outro para encomendas, inclusive uma solicitação direcionada. Documentar o contrato no código da feature.
- [x] 1.3 Conferidos os PNGs e JSONs de avaliar-itinerario e enviar-contraproposta como alternativa autorizada ao MCP. Usar Proposta do cliente no resumo e no modal.

## 2. Backend e testes unitários

- [x] 2.1 Reutilizar ou complementar o backend da branch atual para devolver os dados da modalidade, o preço e as condições persistidos para o ID solicitado. Verificar que duas solicitações com preços distintos produzem respostas distintas. Cobrir acesso autorizado, solicitação de outro destinatário e ausência de valor legado com testes de serviço.
- [x] 2.2 Implementar o serviço de envio, com identidade derivada da sessão e validação de preço, condições e justificativa. Em PropostaServiceTest usar JUnit e Mockito para verificar aceite direto, ajuste, limite de 500 caracteres, espaços em branco, valor inválido e ausência de escrita nos casos rejeitados.
- [x] 2.3 Cobrir ambas as modalidades e prestadores autônomos e organizações. Testar autorização na modalidade errada, solicitação encerrada, direcionada ao próprio prestador ou a terceiro e envio válido de dois prestadores diferentes.
- [x] 2.4 Persistir proposta e notificação na mesma transação. Verificar no PostgreSQL de testes autor, destinatário, condições, mensagem, valor, estado da solicitação e rollback em falha parcial. Verificar repetição do mesmo envio e encerramento entre leitura e gravação.
- [x] 2.5 Expor os endpoints do prestador e verificar com MockMvc sessão ausente, identificadores inválidos, validação, conflito e resposta de sucesso. Executar os testes unitários em backend com .\mvnw.cmd "-Dtest=PropostaServiceTest" test.

## 3. Portal do prestador e Selenium

- [x] 3.1 Implementar tela de avaliação, rota de detalhe e consumo da API com estados de carregamento, erro e confirmação. Comparar com Avaliar itinerário em desktop e conferir leitura e acesso às ações em viewport estreito.
- [x] 3.2 Implementar aceite direto e modal de contraproposta com valor original, novo valor em reais, diferença, justificativa, voltar e enviar. Verificar foco, contador, envio pendente e erro recuperável. Comparar com Enviar contraproposta e verificar a diferença para valores digitados acima e abaixo do preço retornado pela API.
- [x] 3.3 Criar fixtures exclusivas do RF007 e PropostaSystemTest no pacote system do backend, seguindo PrestadorConsultaSystemTest. Abrir a avaliação em sessão autenticada, alternar entre solicitações com ofertas persistidas diferentes, conferir o preço no resumo, botão e modal contra a fixture selecionada, confirmar o valor do cliente para pessoas e conferir proposta, notificação e solicitação ainda aberta por JDBC.
- [x] 3.4 No navegador, abrir o modal, voltar sem enviar, reabrir, tentar envio inválido e então enviar contraproposta válida para encomenda. Conferir valor, mensagem e diferença no fluxo e os registros finais no banco de teste. Simular encerramento após abrir a tela e verificar o erro e a ausência de gravações.
- [x] 3.5 Executar com PostgreSQL de teste (docker compose -f docker-compose.test.yaml up -d), backend em perfil test na porta 8080 e Vite em 5173. No backend executar .\mvnw.cmd "-Dtest=PropostaSystemTest" test. Verificar limpeza das fixtures e encerramento do navegador mesmo em falha.

## 4. Integração

- [x] 4.1 Executar npm run build em frontend e .\mvnw.cmd verify em backend com as dependências de teste ativas. Registrar comandos, resultados reais e falhas, incluindo regressões nos fluxos existentes.
- [x] 4.2 Revisar o diff e as evidências de navegação e persistência. Confirmar que somente arquivos necessários ao prestador e à integração acordada foram alterados.

## 5. Mapa do percurso e caminho planejado

- [x] 5.1 Complementar a API de avaliação com pontos ordenados, coordenadas e geometria viária. Reutilizar coordenadas disponíveis, resolver endereços restantes e obter do OSRM a rota e a duração de cada trecho.
- [x] 5.2 Calcular as chegadas a partir da partida e dos horários planejados. Cobrir com testes unitários trechos de durações diferentes, horário planejado intermediário, chegada ao destino, virada de dia e dados indisponíveis.
- [x] 5.3 Renderizar o mapa Leaflet com base OpenStreetMap, atribuição, zoom, marcadores e enquadramento do percurso. Reproduzir os horários, alinhamentos e conectores do cartão do protótipo em desktop e tela estreita.
- [x] 5.4 Validar no Selenium com Edge a rota, os marcadores, a ordem, os horários e as linhas entre os pontos usando respostas cartográficas e de roteamento controladas. Cobrir trajeto direto, múltiplas paradas e indisponibilidade. Executar o build e os testes pertinentes e registrar os resultados desta revisão.

## 6. Validação após integração com a main

- [x] 6.1 Após integrar a base que inclui 21c7b43, conferir em PostgreSQL de teste a aplicação da V6 do RF004 com V7 e V7.1 da RF007 e o histórico dos bancos que já receberam a RF007. Executar o build do frontend e os testes da RF007, incluindo Selenium no Edge. Registrar a revisão integrada e os resultados.

Validação de 21/11/2026 em cópia isolada de `21c7b43` com os arquivos locais da RF007 sobrepostos. O build do frontend e o Maven verify da RF007 passaram com 21 testes, incluindo sete testes de sistema no Edge. Em PostgreSQL novo, o Flyway aplicou V6 do RF004, V7 e V7.1 em sequência.

O histórico de `app_teste` mostrou V7 e V7.1 aplicadas sem a V6. Em uma cópia do esquema e desse histórico, a configuração padrão do Flyway bloqueou a inicialização pela V6 pendente. Com `outOfOrder=true` apenas nessa cópia, a V6 foi aplicada e os dois testes de integração de propostas passaram. A conciliação da V6 da branch separada do RF011 continua pendente para a equipe.

## Validação do mapa e do caminho planejado

Os 21 testes registrados abaixo foram executados com as alterações locais da RF007 sobre a base e3c3a1c, antes dessa integração.

- O build do frontend e o ESLint dos três arquivos TypeScript alterados passaram. O pacote Leaflet é carregado ao abrir a avaliação.
- Com PostgreSQL de teste, API no perfil test e Vite ativos, o Maven verify da RF007 passou com 21 testes, incluindo sete no Edge. Os três novos cenários de sistema usam o build de frontend/dist, uma API Spring em porta isolada e respostas cartográficas e de roteamento controladas. As capturas ficam em backend/target/rf007-evidencias.
- Para repetir, executar `npm.cmd run build` em frontend. Em backend, definir `$env:SELENIUM_BROWSER='edge'` e executar `.\mvnw.cmd "-Dtest=PercursoServiceTest,PercursoSystemTest,PropostaControllerTest,PropostaServiceTest,PropostaIntegrationTest,PropostaSystemTest" verify`.
- Verificação adicional na API local com Photon e OSRM públicos: a solicitação demonstrativa 908044 em São Paulo retornou três pontos ordenados, geometria com 129 coordenadas e chegadas estimadas às 08:04 e 08:07 para partida às 08:00.

## Validação executada

- Frontend: npm run build e ESLint nos arquivos alterados passaram.
- Backend: Maven verify com PropostaServiceTest, PropostaControllerTest, PropostaIntegrationTest e PropostaSystemTest passou com 16 testes, incluindo quatro no navegador e dois com PostgreSQL. O JAR foi gerado.
- Ambiente: API em perfil test, Vite, PostgreSQL 16 do docker-compose.test.yaml e Edge headless. As três capturas de desktop e tela estreita ficam em backend/target/rf007-evidencias.
- Verificação global no Edge: 83 de 84 testes passaram. FreightHistoryNavigationTest.authenticatedClientConsultsHistoryUsingSessionCookie expirou esperando o link do histórico ficar visível em /cliente/prestadores.
- Integração entre equipes: leitura de valor_cliente e condicoes_cliente validada com fixtures de pessoas e encomendas. A V7 define esses campos de solicitacao_frete como contrato para o módulo de criação.
