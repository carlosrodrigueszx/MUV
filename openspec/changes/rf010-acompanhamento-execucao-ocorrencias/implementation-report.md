# Implementação parcial do RF010

## Etapa inicial de consulta

Gustavo autorizou a implementação dos cenários independentes do RF010 na branch `feat/rf010-acompanhamento-ocorrencias`, incluindo o backend do próprio requisito. A base inicial foi `8c93a99`, com uma worktree e os seis artefatos OpenSpec já existentes como não rastreados. A consulta de PRs abertos não retornou resultados. As migrações da base vão até V10. V11 seria a próxima versão na branch, sem reserva de número ou migração nesta etapa.

- **Consulta operacional:** `GET /api/fretes/{freteId}/execucao`, protegida pela sessão do RF001. Expõe o estado exato, a designação atual, a revisão, a conta autenticada, os registros RF010 existentes e a elegibilidade segundo as regras de domínio. Reusa a verificação de recursos de RF009. A consulta não grava eventos, dados de execução ou avisos.
- **Rota da aplicação:** `/fretes/:freteId/execucao` exibe a consulta por ID real. A troca de frete descarta a consulta anterior. No perfil de motorista, a navegação dessa página não inclui funções administrativas.
- **Formulários:** o componente React cobre as quatro ações com os controles obrigatórios, o efeito previsto, as caixas de confirmação da retomada e do encerramento e os retornos dos três protótipos. O componente recebe a confirmação e o retorno por callback. A rota de leitura não ativa os formulários enquanto faltarem os contratos de gravação. As capturas originais estão nos anexos e em `/home/gustavo/Downloads/1000407677.jpg`, `1000407679.jpg` e `1000407681.jpg`. Os links permanentes seguem em P6.

## Permissões e contratos

| Conta autenticada | Consulta | Início nas regras de domínio | Quatro ações nas regras de domínio |
| --- | --- | --- | --- |
| Prestador responsável | Sim | Apenas se a mesma conta também corresponder ao motorista atual | Sim, em estado elegível |
| Motorista atual | Sim | Em `PRONTO_INICIO` e com recursos válidos | Sim, em estado elegível |
| Colega, motorista substituído sem outro vínculo autorizado, cliente ou prestador alheio | 403 em frete existente | Recusado | Recusado |
| Sem sessão | 401 | Sem autorização | Sem autorização |

Sessão autenticada com frete inexistente recebe 404. O contrato de leitura informa `cicloExecucaoId` ou `interrupcaoId` quando os eventos existentes sustentam a referência. A leitura usa o ID do último evento de transição para identificar esse contexto. Quando não há evento coerente com o estado legado, a consulta continua disponível e as ações que exigem a referência não aparecem em `acoesElegiveis`. O tratamento definitivo desses registros aguarda P5. Os registros lidos preservam os autores originais, inclusive após uma substituição de motorista. Na etapa inicial, a API devolvia elegibilidade sem comandos via POST. A etapa seguinte ligou a interrupção.

As regras de domínio distinguem ocorrência sem mudança de estado, interrupção, retomada e encerramento sem conclusão. Rejeitam nova interrupção em frete interrompido, formulário de ciclo anterior mesmo com o estado e a revisão iguais, retomada ou encerramento contra outra interrupção, recursos inelegíveis para início/retomada e ausência dos campos ou confirmações obrigatórios. O RF009 e o RF011 usam `FOR UPDATE OF f` sobre a mesma linha do frete. A interrupção passou a usar esse mesmo bloqueio na etapa seguinte.

## Verificações

- Maven com PostgreSQL 16 dedicado em localhost, Flyway em esquema isolado e rollback das fixtures entre casos. **63 testes aprovados, 0 falhas, 0 erros, 0 ignorados**. A seleção inclui 6 testes de domínio, 4 de integração RF010 por MockMvc e regressões de compatibilidade RF009, cancelamento RF011 e histórico RF014. Em RF010, as sessões foram obtidas pelo fluxo real do RF001. Os testes verificaram 401/403/404, recursos inválidos, motorista substituído, autoria original e leitura sem alteração de histórico ou avisos.
- `npm run build` e lint em `src/features/execucao` e `src/app/Router.tsx` aprovados. `npm run lint -- --no-cache` apontou dois erros preexistentes em `src/features/driver-linking/index.tsx:4–5`.
- Os 9 testes Node existentes de designação e planejamento passaram com um carregador TypeScript temporário, pois o Node 20 do ambiente não tem `--experimental-strip-types`.
- Chrome headless na rota real com frontend compilado, backend e PostgreSQL. Motorista atual leu o frete interrompido e a justificativa, com navegação restrita. Colega da empresa recebeu 403 e a sessão ausente recebeu 401.
- Chrome headless, componente isolado em configuração de janela larga e estreita. As quatro ações passaram nos testes de campos obrigatórios, caixas de confirmação, controles responsivos, retorno sem callback de confirmação e duplo envio. Isso verifica o formulário em navegador. A confirmação contra a API ainda era pendente na etapa inicial. A interrupção foi verificada no Chrome com a API real na etapa seguinte.
- OpenSpec estrito e checagem de whitespace dos arquivos modificados, incluindo os ainda não rastreados.

Para repetir os testes de backend, use um PostgreSQL exclusivo de teste e configure `MUV_RF010_DB_URL`, `MUV_RF010_DB_USER` e, quando necessário, `MUV_RF010_DB_PASSWORD`. A regressão de integração do RF011 também usa `MUV_RF011_DB_URL`, `MUV_RF011_DB_USER` e `MUV_RF011_DB_PASSWORD` apontando para ambiente de teste. Na pasta `backend`, execute:

```sh
mvn -B -ntp -Dtest=RegrasExecucaoTest,ExecucaoFreteIntegrationTest,CompatibilidadeDesignacaoServiceTest,FreightCancellationRulesTest,FreightCancellationIntegrationTest,FreightHistoryServiceTest test
```

## Arquivos desta etapa

- Backend de produção em `backend/src/main/java/com/muv/backend/execucao/`: `RegrasExecucao.java`, `ExecucaoFreteService.java`, `ExecucaoFreteRepository.java`, `ExecucaoFreteController.java` e `ExecucaoFreteSecurityConfig.java`.
- Testes em `backend/src/test/java/com/muv/backend/execucao/`: `RegrasExecucaoTest.java` e `ExecucaoFreteIntegrationTest.java`. Perfil `backend/src/test/resources/application-rf010-test.properties` e fixture `backend/src/test/resources/db/rf010-consulta.sql`.
- Frontend em `frontend/src/features/execucao/`: `api.ts`, `AcompanhamentoExecucaoPage.tsx` e `FormularioExecucao.tsx`. Rota em `frontend/src/app/Router.tsx`.
- OpenSpec: `tasks.md` e este relatório.

## Interrupção de ponta a ponta

Na branch `feat/rf010-acompanhamento-ocorrencias`, o último número Flyway existente era V10. A nova migração é `V11__rf010_recibo_interrupcao.sql`. Conferir a versão com a equipe antes da integração. A migração acrescenta apenas `rf010_recibo_interrupcao` e foi aplicada em esquema limpo e sobre um esquema V10 com eventos existentes.

- O `POST /api/fretes/{freteId}/execucao/interrupcoes` usa sessão e CSRF. Bloqueia a linha do frete, relê o vínculo da conta e busca o recibo antes da validação de uma nova transição. Estado, evento e recibo são confirmados na mesma transação. O reenvio devolve os mesmos campos e o mesmo ID de evento. Uma nova tentativa em `INTERROMPIDO`, uma chave com dados diferentes e um formulário de ciclo anterior recebem 409.
- O formulário opera na rota já existente. Antes de confirmar, voltar não faz POST. Após resposta incerta, o formulário aberto conserva a chave e o conteúdo do envio e oferece “Tentar novamente”. Após confirmação, a tela consulta novamente o estado vigente. A elegibilidade das demais ações segue nas regras de domínio, sem ativar seus formulários.
- `historico_execucao_frete.data_hora` recebe horário de registro do servidor com `timezone('UTC', clock_timestamp())`, como no RF011. O RF009 pode usar o default do banco e o RF014 lê a coluna diretamente, então essa escolha para o RF010 não redefine o fuso de eventos legados. P2 continua em aberto para a interpretação de negócio, o fuso e a exibição da data e hora digitadas. Por ora, o valor civil é guardado separadamente em `data_hora_informada` no recibo. A coleta e a visibilidade do local da ocorrência seguem P3.
- O histórico compartilhado continua recebendo os campos e tipos já existentes. O RF010 grava o fato de interrupção. O RF013 é responsável por consumir esse fato e entregar avisos, e o RF014 pela apresentação geral.

### Evidências da interrupção

- Maven com PostgreSQL 16 dedicado. **55 testes aprovados, 0 falhas, 0 erros, 0 ignorados**. Inclui 8 testes de interrupção por MockMvc/PostgreSQL, consulta do RF010 e regressões de RF009, RF011 e RF014. Os testes cobrem confirmação pelo motorista e pelo prestador, replay após commit, concorrência com chaves iguais e distintas, recusa após substituição inclusive no reenvio, ciclo antigo após uma retomada representada em fixture, sessão/CSRF e falha forçada no INSERT do recibo com rollback do estado e do evento.
- Chrome headless com frontend compilado, Spring Boot e PostgreSQL. O teste voltou do formulário sem confirmar e só então confirmou. O proxy de teste descartou a primeira resposta após o commit e retornou 502. O navegador reenviou bytes idênticos e recebeu o mesmo resultado. Após recarregar, o frete de teste apareceu como `INTERROMPIDO`. O PostgreSQL confirmou **evento 910075, 1 interrupção, 1 recibo e 0 avisos**.
- Build do frontend, lint de `src/features/execucao` e `src/app/Router.tsx`, validação estrita do OpenSpec e checagem de whitespace aprovados.

Com as variáveis de banco dedicado descritas acima, a seleção Maven desta etapa pode ser reproduzida em `backend`:

```sh
mvn -B -ntp -Dtest=InterrupcaoFreteIntegrationTest,ExecucaoFreteIntegrationTest,RegrasExecucaoTest,FreightCancellationIntegrationTest,CompatibilidadeDesignacaoServiceTest,FreightHistoryServiceTest test
```

Arquivos desta etapa: migração `V11__rf010_recibo_interrupcao.sql`; `InterrupcaoFreteService.java`, `ExecucaoFreteRepository.java`, `ExecucaoFreteController.java` e `ExecucaoFreteSecurityConfig.java`; `InterrupcaoFreteIntegrationTest.java`; `api.ts`, `AcompanhamentoExecucaoPage.tsx`, `FormularioExecucao.tsx` e `InterrupcaoFormulario.tsx`; `design.md`, `tasks.md` e este relatório.

## Retomada de ponta a ponta

A migração aditiva `V12__rf010_recibo_retomada.sql` é a próxima versão desta branch. Conferir a numeração com a equipe antes da integração. A V11 mantém seu conteúdo, inclusive os recibos gravados. A V12 cria uma tabela exclusiva do RF010 para ligar a retomada à interrupção em aberto, sem alterar as tabelas existentes ou o contrato de leitura do RF014.

- O `POST /api/fretes/{freteId}/execucao/retomadas` usa o mesmo serviço transacional da interrupção. Exige sessão, CSRF, autorização atual, revisão, interrupção em aberto do próprio frete, recursos elegíveis conforme RF009, justificativa, data, hora e confirmação de prosseguimento. Grava `EM_EXECUCAO`, o evento `RETOMADA` e o recibo vinculado juntos. A retomada cria um novo ciclo de execução.
- A chave da confirmação é procurada nas duas tabelas sob o bloqueio do frete. Reenvios autorizados com a mesma chave e os mesmos dados devolvem o mesmo ID. Reutilização da chave para outra ação ou outros dados recebe 409. Ao falhar a gravação do recibo, a interrupção anterior permanece em aberto.
- A tela “Retome a execução com segurança” exibe a interrupção em aberto, os recursos confirmados e a caixa obrigatória de prosseguimento. Usa o horário de registro em UTC somente para eventos identificados por recibos do RF010. A data e a hora digitadas continuam separadas no recibo. Seu fuso e sua futura exibição seguem P2. A entrega de avisos continua com o RF013.

### Evidências da retomada

- **58 testes Maven aprovados, sem falhas, erros ou testes ignorados**, incluindo 11 de transição com MockMvc e PostgreSQL, testes de consulta e regras e regressões do RF009, RF011 e RF014. Verificados ambos os autores autorizados, rejeição de recursos inelegíveis, ausência de confirmação, referência alheia, substituição, CSRF, reenvio e rollback por falha no recibo. O caso de formulário antigo agora usa a retomada real, com a mesma designação.
- **Chrome headless em duas abas com frontend compilado, Spring Boot e PostgreSQL**. A primeira aba conservou o formulário antigo. A segunda interrompeu, abriu a retomada, voltou sem POST e verificou que o frete permanecia interrompido. Confirmou a retomada, perdeu a resposta depois do commit e reenviou o mesmo corpo. Após a recarga, o estado era `EM_EXECUCAO`. A primeira aba então recebeu 409 e mostrou a opção “Atualizar acompanhamento”. A tela de retomada foi verificada em desktop e a 390 px. O teste de navegador desta etapa usou Chrome DevTools. A verificação Selenium prevista no checklist permanece pendente.
- **Banco:** para o frete de teste 930063, uma interrupção 930075, uma retomada 930076, um recibo da retomada ligado a 930075 e zero avisos. Os dois resultados da retomada devolveram 930076. A V12 também foi aplicada sobre a V11 com dados, preservando o recibo anterior associado ao evento 910075.
- Build do frontend, lint do RF010 e de `Router.tsx`, OpenSpec estrito e verificação de whitespace passaram.

Com as variáveis de banco dedicado já descritas, executar em `backend`:

```sh
mvn -B -ntp -Dtest=TransicaoFreteIntegrationTest,ExecucaoFreteIntegrationTest,RegrasExecucaoTest,FreightCancellationIntegrationTest,CompatibilidadeDesignacaoServiceTest,FreightHistoryServiceTest test
```

Arquivos desta etapa: `V12__rf010_recibo_retomada.sql`; `TransicaoFreteService.java` (substitui o nome específico de interrupção), `ExecucaoFreteRepository.java`, `ExecucaoFreteService.java`, `ExecucaoFreteController.java` e `ExecucaoFreteSecurityConfig.java`; `TransicaoFreteIntegrationTest.java`; `api.ts`, `AcompanhamentoExecucaoPage.tsx`, `FormularioExecucao.tsx` e `TransicaoFormulario.tsx`; `design.md`, `tasks.md` e este relatório.

## Encerramento sem conclusão de ponta a ponta

A migração aditiva `V13__rf010_recibo_encerramento.sql` era a próxima versão livre na branch. Conferir a numeração antes da integração. Ela cria apenas a tabela de recibos do encerramento. Os arquivos V11 e V12 mantiveram o mesmo conteúdo, e os dados preexistentes das duas tabelas de recibos foram preservados na atualização.

O `POST /api/fretes/{freteId}/execucao/encerramento-sem-conclusao` reutiliza sessão, CSRF, autorização, bloqueio e reenvio das outras transições. O comando confere a interrupção em aberto, a revisão da designação, data, horário, justificativa e confirmação explícita de não conclusão. Grava `ENCERRADO_SEM_CONCLUSAO`, o evento `ENCERRAMENTO_SEM_CONCLUSAO` e o recibo ligado à interrupção em uma transação. O evento recebe a autoria da sessão e o horário de registro em UTC, seguindo o RF011. O valor civil digitado fica no recibo. Os dados de entrega, conclusão, cancelamento e as condições financeiras permanecem sem alteração.

O campo “Situação da encomenda” aparece somente na modalidade de encomendas e é opcional. Quando informado, fica apenas no recibo do RF010 e faz parte da comparação de dados no reenvio. A leitura geral do RF014 continua usando os campos existentes do evento. A eventual visibilidade do campo segue P3. O RF013 permanece responsável pelos avisos.

### Integração PostgreSQL e regressões

- **63 testes de backend aprovados** na seleção de integração e regressão, incluindo 16 em `TransicaoFreteIntegrationTest`, 4 de consulta RF010, 23 de integração RF011 e 20 de regras do RF010 e regressões de RF009 e RF014. Os testes cobrem ambos os autores, recusas, reenvio com o mesmo ID, conflito para conteúdo alterado, ausência de entrega/cancelamento e leitura RF014 sem a situação privada da encomenda.
- A disputa entre retomada e encerramento foi testada nos dois sentidos. O teste verificou que a requisição concorrente aguardava o bloqueio no PostgreSQL. Em ambos os casos, a primeira ação recebeu 200, a segunda recebeu 409 e só houve evento e recibo do desfecho vencedor, além da interrupção original.
- Flyway aplicou a V13 em esquemas limpos e em um banco dedicado já na V12. Os recibos existentes de interrupção e retomada foram comparados antes e depois e permaneceram idênticos.

### Selenium

- **3 testes aprovados, sem falhas, erros ou testes ignorados** em `EncerramentoFreteNavigationTest`, com o WebDriver do projeto, Chrome headless, frontend compilado, Spring Boot e PostgreSQL.
- Em encomendas, o teste descartou a primeira resposta após o commit e verificou que “Tentar novamente” reenviou o mesmo corpo e recuperou o mesmo evento. Após recarregar, o frete apareceu como “Encerrado sem conclusão”, sem entrega concluída ou cancelamento.
- Em frete de pessoas, o prestador encerrou sem campo de encomenda. O teste conferiu o layout em janela estreita e recarregou a página.
- Os retornos “Voltar ao acompanhamento” e “Voltar e manter interrompido” foram verificados sem POST. O PostgreSQL confirmou a mesma interrupção, sem novos eventos, recibos ou avisos.
- A rodada inicial em 8080 encontrou a porta ocupada. A rodada final usou Spring Boot em 18113 e Vite em 18114, com o proxy local atualizado para a porta de teste. O teste aguarda a atualização do conteúdo durante a remontagem da página.

### Inspeção visual

Foram inspecionadas as capturas do Selenium de desktop, tela estreita e após recarga. Os dois cartões empilham na tela estreita. A captura final mostra o encerramento e a interrupção original, sem entrega concluída. Essa leitura de capturas foi complementar aos testes automatizados.

Build, lint dos arquivos do RF010 e de `Router.tsx`, OpenSpec estrito e verificações de whitespace passaram.

Com as variáveis de banco dedicado já descritas, executar em `backend`:

```sh
mvn -B -ntp -Dtest=TransicaoFreteIntegrationTest,ExecucaoFreteIntegrationTest,RegrasExecucaoTest,FreightCancellationIntegrationTest,CompatibilidadeDesignacaoServiceTest,FreightHistoryServiceTest test
mvn -B -ntp -Dtest=EncerramentoFreteNavigationTest -Dmuv.rf010.e2e=true -Dselenium.base-url=http://localhost:5173 test
```

O teste Selenium usa o proxy Vite para localhost:8080 por padrão. Caso a porta esteja ocupada, iniciar o Vite com proxy apontando para a porta escolhida e usar o mesmo valor em `-Dserver.port`. Em ambiente com ChromeDriver externo, informar seu caminho por `-Dwebdriver.chrome.driver`.

Arquivos desta etapa: `V13__rf010_recibo_encerramento.sql`; `TransicaoFreteService.java`, `ExecucaoFreteController.java`, `ExecucaoFreteSecurityConfig.java` e `ExecucaoFreteRepository.java`; `TransicaoFreteIntegrationTest.java` e `EncerramentoFreteNavigationTest.java`; `api.ts`, `AcompanhamentoExecucaoPage.tsx`, `FormularioExecucao.tsx` e `TransicaoFormulario.tsx`; `design.md`, `tasks.md` e este relatório.

## Pendências do checklist

Nesta etapa, foram marcadas **5.2**, **7.2** e **7.3**. O checklist totaliza 14 de 39 tarefas concluídas. Os itens que também exigem outros comandos, integração ou decisões continuam desmarcados. Os IDs P1 a P7 remetem ao [registro de decisões do design](design.md#open-questions).

| Tarefas | O que falta |
| --- | --- |
| 1.4, 6.1–6.5, 7.1, 7.4–7.5 | Ocorrência e início ainda precisam ser ligados aos comandos. A confirmação da retomada em Selenium continua pendente para 7.1. Os retornos da retomada e do encerramento já foram verificados em Selenium. P2, P3 e P6 continuam relevantes para o aceite visual e temporal. Links a partir de RF009/RF014 dependem de alinhamento com Josué. |
| 2.1, 2.3, 2.4 | Interrupção, retomada e encerramento têm recibos próprios e preservam a consulta do RF014. Alinhar eventual tratamento de legados e mudanças compartilhadas (P5). Completar a gravação das demais ações, decidir a exibição do papel com dois vínculos (P1) e fechar a interpretação e a exibição temporal (P2). |
| 3.3, 4.1–4.3, 4.6, 5.3, 5.5–5.6 | A proteção, a gravação e os testes de transação e reenvio passaram para interrupção, retomada e encerramento. A disputa entre retomada e encerramento passou nos dois sentidos. Completar os POSTs restantes, testar ocorrências independentes e verificar a disputa do início contra cancelamento. |
| 4.5, 5.4 | Alinhar com Rafael o consumo dos fatos e o tratamento de duplicatas de aviso em P4. |
| 8.1–8.3 | Completar o aceite integrado das quatro ações e do início. A designação do autônomo continua dependente de RF002/RF009 em P7. |

Nenhum arquivo de implementação do RF003, RF009, RF011, RF013 ou RF014 foi editado. O segundo prompt ficou reservado para a revisão posterior.
