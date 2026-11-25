# Implementação do RF010

## Estado da branch revisada

Na base `26b40a1`, a feature cobre consulta, início, ocorrência sem interrupção, interrupção, retomada e encerramento sem conclusão. A revisão confrontou a US-012, o RF010 e a RN015 com o código, as asserções existentes e as capturas versionadas. A transcrição `VID-20261123-WA0002.pdf` foi usada como referência de organização. Os resultados de execução abaixo pertencem às rodadas registradas no relatório. A revisão documental usou OpenSpec estrito e `git diff --check`.

### Ponto de entrada

O roteador declara `/fretes/:freteId/execucao`. `PercursoExecucaoNavigationTest#contratacaoAteDesfecho` obtém o ID real após a contratação, salva a designação e abre o acompanhamento via WebDriver para motorista e prestador. O item “Fretes” dentro da própria feature aponta para o mesmo frete. A tarefa 6.6 acrescentou e verificou o caminho por clique da visão geral do motorista. As tarefas 6.3, 6.5 e 8.1 cobrem a rota direta e as ações executadas a partir dela.

### Tarefa 6.6 — Entrada pelo portal do motorista

O patch autorizado foi aplicado após `git apply --check`. Na visão geral do motorista, em `/motorista`, a seção **Fretes designados** oferece “Acompanhar execução” para a designação atual da conta em `PRONTO_INICIO`, `EM_EXECUCAO` ou `INTERROMPIDO`.

O controlador compartilhado `PortalOverviewController.java` fornece `fretesAcompanhaveis` na resposta existente da visão geral. `PortalOverview.tsx` monta o link para `/fretes/:freteId/execucao` com o ID real. A agenda conserva a seleção dos quatro próximos fretes com partida futura. O destino continua verificando sessão e vínculo nas regras do RF010.

`PortalExecucaoNavigationTest` usa Chromium headless, o frontend compilado e PostgreSQL 16.14 com schema isolado. O login segue o destino de cada perfil e a fixture complementar ajusta as partidas em relação ao relógio de teste.

| Cenário Selenium | Evidência |
| --- | --- |
| Frete pronto fora da agenda | Os quatro itens da agenda são os fretes com partidas anteriores à de 910061. O motorista clica no link de 910061 na visão geral, chega à URL com esse ID e encontra “Iniciar execução”. |
| Partidas passadas | Por clique na visão geral, 910063 abre como “Em execução”, com ocorrência e interrupção, e 910062 abre como “Interrompido”, com retomada e encerramento sem conclusão. |
| Colega da mesma empresa | Após carregar a visão geral, a conta não recebe os atalhos. As três URLs diretas exibem a recusa de acesso; o backend respondeu HTTP 403. |

A seleção Maven passou com **40 testes, zero falhas, erros ou casos ignorados**: 3 em `PortalExecucaoNavigationTest`, 31 em `TransicaoFreteIntegrationTest` (incluindo a 8.5) e 6 em `ExecucaoFreteIntegrationTest`. A tarefa **6.6** foi marcada com essas evidências. Passaram também o build do frontend, `openspec validate rf010-acompanhamento-execucao-ocorrencias --strict` e `git diff --check`.

### Decisões e pendências atuais

| Decisão | Situação e evidência |
| --- | --- |
| P1 | A conta autenticada é gravada como autora e o acompanhamento exibe seu nome. O início exige o vínculo de motorista e o layout usa o vínculo de prestador quando presente. A exibição de um papel histórico para conta com ambos os vínculos segue em aberto. |
| P2 | Implementada no RF010. Horário civil digitado no recibo e exibido sem conversão, com horário de auditoria em UTC. |
| P3 | Implementada no RF010. V16 habilita local em ocorrência/interrupção. Situação da encomenda cabe ao encerramento dessa modalidade. Os testes verificam os dados operacionais e sua ausência na projeção geral do RF014. |
| P4 | IDs de eventos e reenvio idempotente disponíveis. Aceite do consumo, destinatários e entrega aguardam Rafael (RF013), tarefas 4.5 e 5.4. |
| P5 | Recibos preservam estados e referências. Legado sem abertura verificável permanece legível com os comandos dependentes de ciclo indisponíveis. `contratacaoAteDesfecho` verifica a projeção atual do RF014. A evolução do contrato compartilhado aguarda Josué, tarefas 2.1 e 2.3. |
| P6 | Comparação documental concluída com as três referências desktop versionadas e os dois anexos adicionais. As capturas de 390 px e as asserções Selenium registram a responsividade. |
| P7 | O teste existente documenta recusa 422 na designação do autônomo e permanência em `AGUARDANDO_DESIGNACAO`. O percurso real aguarda RF002/RF009, tarefa 8.2. |

O checklist ativo registra **36 de 36 tarefas do escopo desta entrega**, incluindo **6.6** e **8.5**. Cinco tarefas foram encaminhadas aos requisitos responsáveis, com aceite ainda pendente. O papel exibido no histórico (P1) e o percurso real do autônomo (P7) seguem em aberto.

### Tarefas encaminhadas aos requisitos responsáveis

As tarefas abaixo saíram do checklist ativo desta entrega. A tabela conserva os IDs originais, as evidências já disponíveis e o aceite a obter junto aos responsáveis de RF014, RF013 e RF002/RF009.

| ID original · decisão | Critério de aceite | Evidência disponível | Aceite pendente e responsáveis |
| --- | --- | --- | --- |
| 2.1 · P5 | Registrar o contrato aprovado sobre histórico compartilhado, dados legados e responsabilidade por mudanças. | `consultaLegadoEmExecucaoSemEventoDeAbertura` comprova consulta preservada com ações dependentes de ciclo indisponíveis. O RF014 lê `historico_execucao_frete` e ordena por data e ID. | Registro do acordo sobre fonte oficial dos metadados e comportamento compartilhado para eventos sem recibo ou sem abertura verificável. Josué (RF014), Gustavo e Walison (RF010). |
| 2.3 · P5 | Validar os metadados de auditoria que o contrato aprovado expõe aos perfis autorizados. | Os recibos RF010 preservam estados, referência de ciclo ou interrupção e identidade da confirmação. Os testes de transição verificam vínculo e reenvio. A projeção atual do RF014 retorna data, tipo, autor, descrição e justificativa. | Contrato da 2.1 e um teste conjunto que confirme os campos acordados para eventos novos e legados, incluindo visibilidade por perfil. Josué conduz mudanças no RF014, com Gustavo e Walison na origem dos dados. |
| 4.5 · P4 | Acordar como o RF013 recebe interrupção, retomada e encerramento confirmados, com política de destinatários e falhas. | Cada confirmação aceita tem um ID persistido. A referência local `origin/feat/rf013-notificacoes-internas` (`e62a986`) tem publicador e observador de solicitações com destinatário resolvido, sem ID do evento de origem. A tarefa 1.4 do RF013 prevê conectar execução/histórico. | Contrato aceito por Rafael (RF013), Gustavo e Walison (RF010) sobre mecanismo de consumo, identidade do fato, destinatários, falha e deduplicação. O observador consultado ainda não consome os três fatos do RF010. |
| 5.4 · P4 | Demonstrar que o RF013 consome os fatos e entrega apenas os avisos previstos, inclusive em reenvio e falha. | O teste de retomada recupera o ID original. O de encerramento verifica o reenvio e a contagem de eventos. Ambos comprovam idempotência na origem. | Após 4.5, integração de evento a aviso que confira destinatários, total por evento e destinatário após reenvio e o resultado de falha conforme a política combinada. Rafael valida o consumo com Gustavo e Walison. |
| 8.2 · P7 e P1 | Completar o percurso real do autônomo com designação, início e ações posteriores, conferindo autoria e a apresentação acordada em P1. | `designacaoDoAutonomoAindaBloqueiaOFluxoAntesDoInicio` comprova HTTP 422 e `AGUARDANDO_DESIGNACAO`. O cadastro atual grava o perfil em `prestador_autonomo` sem criar a associação em `motorista.usuario_id`. `DesignacaoService` ainda rejeita a designação do autônomo. A 8.5 comprova a autorização e a autoria em uma fixture de duplo vínculo. | RF002 precisa disponibilizar a associação real da conta ao motorista próprio e RF009 precisa persistir a designação válida até `PRONTO_INICIO`. Depois, Gustavo e Walison podem validar o percurso normal em PostgreSQL e Selenium com os responsáveis de RF002/RF009. O papel exibido depende da decisão P1. |

P1 permanece como decisão pendente sobre o papel exibido no histórico. O RF010 grava `usuario_autor_id` e exibe “Por” seguido do nome da conta. A 8.5 cobre a identidade persistida no duplo vínculo. Gustavo e Walison ainda precisam definir qual papel exibir e como preservar seu significado histórico após troca de vínculo. Josué participa se a decisão afetar a projeção geral ou o esquema (P5). P1 também afeta o aceite da apresentação na 8.2, enquanto P7 bloqueia o próprio percurso. P5 vincula 2.1 e 2.3, e P4 vincula 4.5 e 5.4.

Na revisão, passaram **44 testes**, sem falhas, erros ou casos ignorados, em JDK 21 e PostgreSQL 16 de teste. Foram 31 de transição RF010, 6 de consulta RF010 e 7 do serviço de histórico RF014. OpenSpec estrito e `git diff --check` também passaram.

### Tarefa 8.5 — Conta responsável e motorista designada

`TransicaoFreteIntegrationTest#contaResponsavelIniciaComoMotoristaDesignadoEPermaneceAutora` reutiliza o frete de pessoas 910061, o motorista verificado 910011 e o veículo elegível 910021 da fixture `rf010-consulta.sql`. A conta responsável 910002 autentica-se pelo login normal e confirma as ações com CSRF. No controle, o motorista designado está associado a outra conta. O POST de início responde HTTP 403, o frete permanece em `PRONTO_INICIO` e as consultas SQL verificam a ausência de evento, recibo e aviso.

Após associar o motorista da fixture à conta 910002, a mesma sessão inicia o frete. O cenário confirma ocorrência, interrupção, retomada, nova interrupção e encerramento sem conclusão. Para cada confirmação, o teste verifica o resultado da API, o estado em `frete` e a autoria em `historico_execucao_frete` pelo ID do evento. Todos os seis eventos registram `usuario_autor_id = 910002`. O teste repõe o vínculo original ao sair.

Execução em JDK 21 e PostgreSQL 16.14 temporário, com cópia limpa dos fontes do backend e schemas isolados pelo Flyway. O teste novo passou isoladamente antes da suíte. Em seguida, passaram os **37 testes de integração do RF010** (31 de transição e 6 de consulta), sem falhas, erros ou casos ignorados.

```bash
mvn -B -ntp '-Dtest=TransicaoFreteIntegrationTest#contaResponsavelIniciaComoMotoristaDesignadoEPermaneceAutora' test
mvn -B -ntp -Dtest=ExecucaoFreteIntegrationTest,TransicaoFreteIntegrationTest test
```

`openspec validate rf010-acompanhamento-execucao-ocorrencias --strict` e `git diff --check` passaram.

## Histórico das verificações

### Etapa inicial de consulta

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

## Pendências ao término do encerramento

Nesta etapa, foram marcadas **5.2**, **7.2** e **7.3**. O checklist totaliza 14 de 39 tarefas concluídas. Os itens que também exigem outros comandos, integração ou decisões continuam desmarcados. Os IDs P1 a P7 remetem ao [registro de decisões do design](design.md#open-questions).

| Tarefas | O que falta |
| --- | --- |
| 1.4, 6.1–6.5, 7.1, 7.4–7.5 | Ocorrência e início ainda precisam ser ligados aos comandos. A confirmação da retomada em Selenium continua pendente para 7.1. Os retornos da retomada e do encerramento já foram verificados em Selenium. P2, P3 e P6 continuam relevantes para o aceite visual e temporal. Links a partir de RF009/RF014 dependem de alinhamento com Josué. |
| 2.1, 2.3, 2.4 | Interrupção, retomada e encerramento têm recibos próprios e preservam a consulta do RF014. Alinhar eventual tratamento de legados e mudanças compartilhadas (P5). Completar a gravação das demais ações, decidir a exibição do papel com dois vínculos (P1) e fechar a interpretação e a exibição temporal (P2). |
| 3.3, 4.1–4.3, 4.6, 5.3, 5.5–5.6 | A proteção, a gravação e os testes de transação e reenvio passaram para interrupção, retomada e encerramento. A disputa entre retomada e encerramento passou nos dois sentidos. Completar os POSTs restantes, testar ocorrências independentes e verificar a disputa do início contra cancelamento. |
| 4.5, 5.4 | Alinhar com Rafael o consumo dos fatos e o tratamento de duplicatas de aviso em P4. |
| 8.1–8.3 | Completar o aceite integrado das quatro ações e do início. A designação do autônomo continua dependente de RF002/RF009 em P7. |

Nenhum arquivo de implementação do RF003, RF009, RF011, RF013 ou RF014 foi editado. O segundo prompt ficou reservado para a revisão posterior.

## Ocorrência sem interrupção e verificação da US-012

Base local `79368da`. O formulário do acompanhamento envia a descrição, a data, o horário, a revisão e o ciclo ao novo POST `/api/fretes/{freteId}/execucao/ocorrencias`. A migração V14 cria o recibo da ocorrência. Sob bloqueio do frete, o serviço revalida a autoria e recupera o resultado de uma confirmação já gravada. Uma chave nova registra outro evento, mesmo com descrição e horário iguais. Evento e recibo são gravados juntos. O estado anterior e o resultante são `EM_EXECUCAO`.

- Com JDK 21 e PostgreSQL 16 dedicado, passaram os 64 testes da seleção Maven documentada na etapa de encerramento. A execução usou uma cópia limpa dos fontes porque o diretório local `target` continha migrações antigas. O novo teste de integração usa o motorista designado e confere reenvio, conflito com descrição alterada, segundo ID, dois recibos e estado após nova consulta.
- Em um teste Selenium temporário com o prestador, voltar sem registrar fez zero POSTs. Após a perda simulada da primeira resposta depois do commit, “Tentar novamente” reenviou o mesmo corpo e recuperou o mesmo ID. Um segundo formulário com os mesmos dados gerou outro ID. Após recarregar, o frete continuava em execução com dois eventos adicionais no banco.
- Build do frontend, ESLint da feature, OpenSpec estrito e checagem de whitespace passaram. Foram marcados 4.6 e 6.4, considerando também as verificações anteriores da interrupção. A cobertura de ocorrência nas duas modalidades exigida em 4.3 permanece pendente.
- US-012: a consulta de um frete em `PRONTO_INICIO` retornou `INICIO` para o motorista designado e nenhuma ação para a conta distinta do prestador. As regras de domínio rejeitaram recursos inválidos. Ainda faltam o POST de início, a ação na tela, o evento persistido e o teste de concorrência com cancelamento (4.2, 6.3 e 5.6).

## Início da execução pela US-012

A etapa partiu da árvore local de `feat/rf010-acompanhamento-ocorrencias` em `79368da`, com a ocorrência sem interrupção já modificada e a V14 presente. A V15 acrescenta somente `rf010_recibo_inicio`. Conferir sua numeração antes da integração.

Na rota `/fretes/:freteId/execucao`, o botão “Iniciar execução” segue a lista de ações da consulta. A confirmação mostra o efeito no frete e envia `chaveOperacao`, `estadoEsperado` e `revisaoDesignacao` para `POST /api/fretes/{freteId}/execucao/inicio` com sessão e CSRF. O serviço bloqueia a linha do frete, relê a designação e verifica o vínculo do motorista antes de consultar o recibo. Para um início novo, confere o estado, a revisão, o motorista verificado e a compatibilidade do veículo pelo RF009. Estado, histórico com autoria da sessão e recibo são gravados na mesma transação. O ID de `INICIO` passa a ser a referência do ciclo para a primeira ocorrência.

### Evidências de persistência e concorrência

- Em PostgreSQL 16 com Java 21, passaram 71 testes na seleção de backend, incluindo 24 testes de comandos RF010 e as regressões de RF009, RF011 e RF014. A execução partiu de uma cópia limpa de `backend/src` e `pom.xml`, com esquemas novos por suíte.
- O POST recusou sessão ausente, falta de CSRF, prestador sem vínculo de motorista, colega, motorista substituído, revisão antiga e veículo incompatível. A troca de motorista após a consulta invalidou a confirmação aberta. Mesmo após um início válido, o motorista substituído recebeu 403 ao reenviar. Uma falha induzida na inserção do recibo reverteu evento e estado. Retirada a falha, a mesma confirmação funcionou.
- A disputa usou o POST real de cancelamento do RF011, uma transação por concorrente e verificação de espera por bloqueio no PostgreSQL. Com início vencedor, o cancelamento recebeu 409 e restaram `EM_EXECUCAO`, um evento do motorista, um recibo e zero avisos. Com cancelamento vencedor, o início recebeu 409 e restaram `CANCELADO`, um evento do cliente, um aviso e zero recibos de início.
- A API de ocorrência recusou descrição composta por espaços Unicode, data ou horário ausente e horário inválido nas duas modalidades. Em cada modalidade, a confirmação válida produziu um recibo vinculado ao ciclo, com estados anterior e resultante iguais a `EM_EXECUCAO`.

### Selenium e frontend

Passaram os quatro testes de navegação, incluindo os três de regressão do encerramento. No frete de pessoas 910061, o motorista abriu a confirmação e a designação mudou antes do clique. O navegador recebeu a recusa e o banco continuou sem evento. Após nova designação e atualização da tela, a primeira resposta de sucesso foi descartada depois do commit. “Tentar novamente” recuperou o mesmo ID. O motorista registrou a ocorrência e recarregou o acompanhamento. O SQL confirmou início **910075**, ocorrência **910076**, ciclo da ocorrência **910075** e frete em **EM_EXECUCAO**. A consulta encontrou exatamente esses dois eventos no frete.

Passaram os nove testes Node de designação e planejamento, o build do frontend, o ESLint da feature, a validação estrita do OpenSpec e a verificação de whitespace.

### Reprodução

Usar banco de teste exclusivo e as variáveis `MUV_RF010_DB_*` e `MUV_RF011_DB_*` já descritas. Para Selenium, servir o build com proxy para o backend de teste e indicar a URL do frontend em `selenium.base-url`. Nesta máquina foram usados Chromium headless e ChromeDriver local.

```sh
# backend, com JDK 21
mvn -B -ntp -Dtest=RegrasExecucaoTest,ExecucaoFreteIntegrationTest,TransicaoFreteIntegrationTest,CompatibilidadeDesignacaoServiceTest,FreightCancellationIntegrationTest,FreightHistoryServiceTest test
mvn -B -ntp -Dtest=InicioFreteNavigationTest,EncerramentoFreteNavigationTest -Dmuv.rf010.e2e=true -Dselenium.base-url=http://127.0.0.1:18114 test

# frontend, com Node 24
npm run build
./node_modules/.bin/eslint src/features/execucao
node --test --test-isolation=none test/*.test.mjs

# raiz
openspec validate rf010-acompanhamento-execucao-ocorrencias --strict
git diff --check
```

### Checklist após o início

Itens fechados nesta etapa: 4.2, 4.3, 5.6, 6.3 e 8.3. O checklist fica com 21 de 39 concluídos.

| Quem pode fechar | Itens abertos e evidência ou decisão restante |
| --- | --- |
| Gustavo e Walison na própria branch | **3.3, 4.1, 5.3, 5.5** — completar a matriz de contratos, autoria enviada pelo cliente, recusas, rollback e formulários antigos em ciclos sucessivos. **6.1, 6.5, 7.1, 7.4** — verificar troca de frete, pontos de entrada já autorizados, retomada confirmada em Selenium e falha de atualização após sucesso. Links que exijam editar RF009 ou RF014 precisam de alinhamento. **8.1** — percorrer contratação e designação reais nas duas modalidades, com retomada e encerramento em caminhos distintos. |
| Gustavo e Walison, com as referências visuais | **7.5** — comparar as três telas e verificar teclado e foco. Os dois podem resolver P6 disponibilizando uma referência aprovada. |
| Com Josué (RF014) | **2.1, 2.3** — fechar P5 sobre histórico e legados. **2.4** — decidir P2 sobre horário e exibição. **1.4, 6.2** — fechar P2/P3 para os campos e obter as referências de P6. |
| Com Rafael (RF013) | **4.5, 5.4** — acordar P4 sobre consumo dos eventos e duplicatas de avisos. |
| Após liberação por RF002/RF009 | **8.2** — executar o fluxo de motorista autônomo (P7) e confirmar a apresentação da autoria quando a conta reúne os dois papéis (P1). |

## Cobertura por critério após a US-012

A revisão usou a árvore local da branch em `79368da`, incluindo V14 e V15. Os nomes abaixo identificam métodos JUnit que podem ser selecionados pelo Surefire. Os testes HTTP obtêm sessões pelo login normal. Os testes Selenium usam Chromium, o frontend compilado, Spring e PostgreSQL 16. As contas, endereços, propostas e recursos são dados controlados de teste.

| Item | Critério e teste que o comprova |
| --- | --- |
| 3.3 | `sessaoCsrfEAutorDoContrato` cobre os cinco POSTs anônimos com CSRF válido (401), ausente e inválido (403). A interrupção com campos extras de autor e papel devolve e grava a conta da sessão. `rejeitaColegaERevalidaAutorizacaoAposSubstituicaoAteNoReenvio` cobre falsificação por terceiro e replay após substituição. `motoristaConfirmaERecuperaARespostaDepoisDoCommit` compara a resposta serializada completa, ID, autor e estados. `exigeSessaoCsrfEJustificativa` e `prestadorPodeConfirmarEChaveUsadaOuNovaTentativaGeramConflito` distinguem 400 de 409. |
| 4.1 | `distinguePrestadorMotoristaEContasAlheiasEmTodosOsEstados` cobre a matriz das cinco ações por estado e vínculo. `inicioRevalidaSessaoRevisaoMotoristaERecursosAntesDaEscrita` confere revisão e designação atuais. `inicioECancelamentoDisputamOMesmoFrete` e `retomadaEEncerramentoConcorrentesGravamSomenteOVencedor` verificam bloqueio real nas duas ordens. Falha no INSERT do recibo: `falhaNoReciboDeInicioMantemFretePronto`, `falhaDoReciboReverteEstadoEEventoEPermiteReenvio`, `falhaNoReciboDeRetomadaMantemAInterrupcaoEPermiteReenvio` e `falhaNoReciboReverteOcorrenciaOuEncerramento` verificam rollback e nova tentativa nas cinco ações. |
| 5.3 | `cadaRecusaPreservaEstadoEventosEAvisos` submete cada comando nos três estados terminais e em `AGUARDANDO_DESIGNACAO`. Também submete retomada e encerramento em execução ativa e, com interrupção aberta, justificativas em branco e confirmação nula. Cada rejeição é seguida de SQL sobre estado, quantidade de eventos e avisos do frete. Os valores de confirmação falsos são cobertos por `validaConfirmacaoReferenciaRecursosEAutorizacaoNoMomentoDaRetomada` e `exigeInterrupcaoConfirmacaoEAutorizacaoParaEncerrar`. |
| 5.5 | `formulariosDeDuasInterrupcoesComMesmaDesignacao` recusa ocorrência/interrupção do ciclo anterior e retomada/encerramento da primeira interrupção quando a segunda está aberta. `formularioDeCicloAnteriorFalhaMasReenvioOriginalContinuaValidoAposRetomada` distingue o reenvio da confirmação já gravada. `validaConfirmacaoReferenciaRecursosEAutorizacaoNoMomentoDaRetomada` verifica a troca de motorista. `ciclosNavegacaoTecladoERecuperacao` mantém abertos os dois formulários em abas distintas durante uma interrupção e retomada reais. Ambos recebem conflito e oferecem atualização. |
| 6.1 e 6.5 | `ciclosNavegacaoTecladoERecuperacao` troca do frete 910063 para 910061, confirma ID e estado novos e o descarte do formulário anterior. `contratacaoAteDesfecho` entra por `/cliente/solicitacoes/:id/propostas`, recebe o ID de frete criado, acessa a designação por `/prestador/fretes/:id/designacao` e o RF010 por `/fretes/:freteId/execucao`. O motorista entra diretamente no acompanhamento. O teste verifica a ausência dos links administrativos de veículos e motoristas. Os retornos de retomada e encerramento são cobertos por `voltarDaRetomadaEDoEncerramentoPreservaAInterrupcaoSemPost`. O roteador compartilhado e os módulos dos colegas permaneceram intactos. |
| 7.1 | `ciclosNavegacaoTecladoERecuperacao` verifica motivo e autor da interrupção, recursos e controles da retomada, disposição dos cartões a 390 px e confirmação pelo navegador. `contratacaoAteDesfecho` verifica o autor real da retomada no banco, nas duas modalidades, e recarrega o frete em execução. A comparação com a imagem aprovada continua registrada separadamente em 7.5. |
| 7.4 | `ciclosNavegacaoTecladoERecuperacao` provoca uma falha no GET que sucede a retomada confirmada. Resultado persistido e erro de consulta aparecem juntos. “Tentar novamente” refaz a leitura e recoloca o foco no recibo. Em conflito, o usuário atualiza, reabre a mesma ação e pode revisar o texto. A consulta SQL ao fim confirma que a atualização e a reabertura não enviaram os rascunhos. `motoristaIniciaFreteDePessoasERegistraOcorrenciaNoMesmoCiclo` cobre alteração de designação depois da abertura. |
| 8.1 | `contratacaoAteDesfecho` executa quatro combinações: pessoas/retomada, pessoas/encerramento, encomenda/retomada e encomenda/encerramento. O ponto de partida de cada teste é uma solicitação aberta com uma proposta enviada. O cliente aceita pela tela, o prestador designa, o motorista inicia, o prestador registra ocorrência, o motorista interrompe e o prestador escolhe o desfecho. Após recarga, o teste verifica a situação mostrada e a sequência de cinco eventos com autores no PostgreSQL. A sessão do cliente consulta o endpoint normal do RF014 e encontra os quatro fatos de execução na linha do tempo daquele frete. Nenhum percurso parou em um módulo de colega. |

### Correções verificadas no RF010

O rascunho de uma ação em conflito era perdido ao atualizar. Agora a tela guarda seus campos em memória para revisão no mesmo frete e na mesma ação. O relato, a data e o horário podem ser recuperados ao reabrir. A confirmação e a chave são novas. Voltar, trocar de ação ou trocar de frete descarta o rascunho. O foco vai ao título ao abrir o formulário e à mensagem pertinente após erro ou confirmação. Uma ação nova limpa o recibo anterior. A validação do backend também cobre o caractere de espaço Unicode U+0085, verificado por `validaTextosEConfirmacoesSemLimitarDatas`.

### Teclado, capturas e dependências

Em `ciclosNavegacaoTecladoERecuperacao`, o foco inicial é o título. Tab percorre data, horário, texto, situação da encomenda quando exibida, confirmação quando exigida, envio e retorno. Shift+Tab retorna ao envio e Enter com campos vazios leva o foco ao primeiro campo obrigatório, a data, nos três formulários. O teste de recuperação verifica o foco no alerta de conflito, no erro de consulta e no resultado confirmado. Os cartões empilham a 390 px. As capturas do RF010 estão em [evidencias/](evidencias/), em larguras de janela de 1360 e 390 px.

**Naquela rodada, 0 de 3 telas comparadas.** O documento de arquitetura disponível aponta para o protótipo geral (nó 11-2). Os três JPEGs identificados no design não foram localizados no projeto ou nos Downloads e não foi possível identificar os três frames aprovados nesse link. O arquivo local “MUV - Protótipo.zip” contém somente cadastro e autenticação. As capturas desta rodada registram a aplicação e ficam disponíveis para comparação quando a referência for confirmada.

| Responsável | Pergunta para fechar o aceite |
| --- | --- |
| Gustavo e Walison, P6 | Quais são os links permanentes ou as três imagens aprovadas de registro/interrupção, retomada e encerramento? Com essas referências, a dupla pode concluir 7.5 e a parte visual de 1.4 e 6.2 na branch. A comparação posterior consta abaixo. |
| Gustavo e Walison com Josué, P2/P3 | Qual fuso e significado têm a data e a hora informadas frente ao instante de auditoria? Onde guardar e para quem exibir o local e a situação da encomenda? Isso afeta 1.4, 2.4 e 6.2. |
| Josué com a dupla, P5 | O RF014 aceita os recibos RF010 como fonte de estados e vínculo de ciclo? Como tratar históricos legados sem evento que abra o ciclo? Falta esse acordo para 2.1 e 2.3. |
| Rafael com a dupla, P4 | Para quais fatos e destinatários o RF013 deve emitir avisos e qual identificador usará para evitar duplicatas em reenvios? 4.5 e 5.4 aguardam o contrato. |
| RF002/RF009 e dupla, P7/P1 | Qual fluxo de cadastro e designação associará a conta do autônomo ao motorista próprio e permitirá chegar a PRONTO_INICIO? Para 8.2, a dupla precisa exercitar esse fluxo e definir o papel exibido quando ambos os vínculos coincidem. |

### Verificações da rodada

Para repetir a seleção de navegador documentada acima, incluir `PercursoExecucaoNavigationTest` em `-Dtest` junto de `InicioFreteNavigationTest` e `EncerramentoFreteNavigationTest`. Os testes geram PNGs em `target/rf010-browser`. Em uma execução intermediária, uma recompilação simultânea no mesmo diretório de build interferiu na inicialização das rotas da regressão de encerramento. Na cópia limpa dos fontes finais, as três classes Selenium terminaram com 9 testes aprovados (5 de percurso, 1 de início, 3 de encerramento). O processo combinado foi encerrado posteriormente durante o backend. A seleção de backend foi executada separadamente.

Resultado final: **76 testes de backend, 9 Selenium e 9 Node aprovados**, sem falhas, erros ou casos ignorados nas seleções. Passaram também o build do frontend, o ESLint da feature, OpenSpec estrito e `git diff --check`. Foram fechados **3.3, 4.1, 5.3, 5.5, 6.1, 6.5, 7.1, 7.4 e 8.1**. O checklist passou de 21 para **30 de 39**. Permanecem abertos **1.4, 2.1, 2.3, 2.4, 4.5, 5.4, 6.2, 7.5 e 8.2**.

### Comparação com os três JPEGs aprovados

As referências fornecidas pela dupla estão preservadas em `evidencias/referencias/`. O pareamento foi feito pelos títulos visíveis no conteúdo desktop dos JPEGs. Os nomes, placas, horários, frete e dados de encomenda da referência são exemplos. A aplicação usa os dados da sessão e do frete.

| Referência e tela | Diferenças encontradas na captura anterior e ajustes no RF010 |
| --- | --- |
| [1000407677.jpg — Registre uma ocorrência](evidencias/referencias/1000407677.jpg) · [Desktop](evidencias/interrupcao-1360.png) · [390 px](evidencias/interrupcao-390.png) | O tipo estava fixo e faltavam o título “O que aconteceu?”, os indicadores de obrigatoriedade, o local e a explicação do histórico. O seletor agora permite ocorrência e interrupção conforme a elegibilidade. Tipo, data, horário e descrição ou justificativa são obrigatórios. O painel “Efeito deste registro” mostra a consequência da seleção e os recursos atuais. No modo de interrupção, as ações são “Registrar e interromper execução” e “Voltar sem registrar”. Naquela captura, o local ainda estava desabilitado. A V16 habilitou o campo opcional. |
| [1000407679.jpg — Retome a execução com segurança](evidencias/referencias/1000407679.jpg) · [Desktop](evidencias/retomada-1360.png) · [390 px](evidencias/retomada-390.png) | Faltavam o título “Registro de retomada”, as marcas nos campos e o texto sobre inclusão da justificativa no histórico. À esquerda ficam data, horário, justificativa, recursos compatíveis, aceite obrigatório de prosseguimento e “Confirmar retomada”. À direita ficam a interrupção em aberto com motivo e autoria, o efeito “Após confirmar: Em execução” e “Voltar ao acompanhamento”. O subtítulo usa o horário de registro disponível em UTC. A decisão P2 foi aplicada posteriormente à apresentação do horário civil informado. |
| [1000407681.jpg — Encerre sem marcar como concluído](evidencias/referencias/1000407681.jpg) · [Desktop](evidencias/encerramento-1360.png) · [390 px](evidencias/encerramento-390.png) | A captura anterior trazia interrupção e recursos extras no painel de revisão. O formulário ganhou “Motivo do encerramento”, a orientação sobre impossibilidade de retomada e as marcas nos campos obrigatórios. Data, horário, justificativa e aceite de não conclusão são exigidos. A situação da encomenda é opcional e específica da modalidade. O painel “Revise antes de confirmar” destaca “Esta ação não é uma conclusão”, o registro do motivo e do responsável, a ausência de entrega e o autor autenticado. A confirmação vermelha e “Voltar e manter interrompido” mantêm as ações do JPEG. |

Nos três casos, o formulário fica à esquerda e o painel explicativo à direita no desktop. A identificação RF010 / MUV foi restabelecida. As capturas de teste usam o portal restrito do motorista, enquanto os JPEGs mostram o portal do prestador. O layout de conta permanece vinculado ao perfil real. Os avisos descrevem os efeitos persistidos pelo RF010. A promessa de notificar destinatários exibida nos três JPEGs depende do contrato com RF013 (P4). Os campos começam vazios e os aceites desmarcados para exigir confirmação do usuário.

A 390 px, o critério é responsividade: o painel de efeito ou revisão passa para baixo do formulário e os controles permanecem acessíveis pela rolagem. Os JPEGs são referências apenas de desktop.

O build, o ESLint de `src/features/execucao` e o cenário Selenium `PercursoExecucaoNavigationTest#ciclosNavegacaoTecladoERecuperacao` passaram nesta revisão. O cenário percorreu os controles com Tab e Shift+Tab, conferiu o foco no campo obrigatório, nos alertas e no recibo, verificou os retornos e a recuperação da consulta, e confirmou o empilhamento a 390 px. As seis capturas foram renovadas e inspecionadas. O painel da direita exige rolagem na janela estreita. A execução mais ampla de Selenium foi interrompida e o resultado desta revisão corresponde ao cenário isolado.

A comparação com os três JPEGs conclui P6 e a tarefa **7.5**. O checklist passa a **31 de 39**. **1.4** e **6.2** seguem abertas pelas decisões P2 e P3, com a parte visual registrada acima. A política de notificações segue P4.

### Conferência com os anexos de registro e retomada

O anexo `1000407926.jpg` confirma a disposição de “O que aconteceu?” e “Efeito deste registro”, com local opcional e ação de retorno. A captura atual mostra o local habilitado e reúne ocorrência e interrupção no seletor. O anexo `1000407928.jpg` confirma a disposição de “Registro de retomada” e “Interrupção em aberto”, com justificativa, recursos, aceite de prosseguimento e retorno. As capturas desktop mostram os pares de cartões. Nas capturas estreitas, o painel complementar fica abaixo do formulário e pode exigir rolagem. Para encerramento, a comparação usa o JPEG versionado `1000407681.jpg`, pois os dois anexos desta revisão cobrem as outras telas.

As referências mostram o portal do prestador com valores preenchidos e mensagens de notificação. As evidências Selenium usam o portal do motorista, dados de teste, campos inicialmente vazios e avisos do efeito sobre o frete. Retomada e encerramento são ações próprias no acompanhamento de um frete interrompido. A comparação confirma estrutura e comportamento previstos para o RF010, com essas diferenças de perfil, conteúdo e navegação. A entrega dos avisos permanece em P4.

### Pendências registradas antes da V16

O fechamento depende das decisões abaixo. Os testes existentes de persistência e idempotência verificam o comportamento sob os contratos atuais.

| Tarefas | Evidência e decisão necessária |
| --- | --- |
| 1.4, 2.4, 6.2 | A inspeção dos JPEGs e a validação de data e hora obrigatórias estão concluídas. A data e hora informadas ficam no recibo, separadas do instante de registro. Para P2, a proposta é preservar o valor civil informado no recibo, manter o instante de auditoria em UTC e definir qual horário o RF014 deve apresentar. Para P3, a proposta é guardar o local opcional e a situação da encomenda em dados privados do RF010, com acesso pelo prestador e pelo motorista designado. A coleta de local segue desabilitada até a decisão. |
| 2.1, 2.3 | Os recibos ligam eventos, estados e referências de ciclo. Os percursos do RF010 confirmam a leitura dos fatos pelo endpoint do RF014. Falta Josué aprovar a semântica compartilhada e o tratamento de legados. A proposta é manter a leitura de registros antigos e condicionar comandos que exigem ciclo à existência de evento de abertura verificável. |
| 4.5, 5.4 | Interrupção, retomada e encerramento já gravam um único fato por confirmação e o replay recupera o identificador original. A proposta para Rafael é consumir esses fatos pelo ID de `historico_execucao_frete` e deduplicar a entrega por evento e destinatário. Falta acordar destinatários e política de reenvio do RF013. |
| 8.2 | A designação do autônomo depende de RF002/RF009. A proposta para P1 é usar a conta autenticada como autora e apresentar o papel de motorista para o início, que exige esse vínculo. O percurso completo precisa da associação real entre a conta e o motorista e da designação liberada no RF009. |

Passaram **42 testes** com PostgreSQL de teste. `consultaLegadoEmExecucaoSemEventoDeAbertura` confirmou que o histórico existente continua legível e que um ciclo sem abertura verificável fica sem ações disponíveis. `designacaoDoAutonomoAindaBloqueiaOFluxoAntesDoInicio` confirmou HTTP 422 no POST de designação e frete em `AGUARDANDO_DESIGNACAO` após a recusa. OpenSpec estrito e `git diff --check` passaram. O checklist permanece em **31 de 39** até o aceite das decisões e a liberação da dependência.

### Decisões de horário e campos privados

O horário civil informado aparece no acompanhamento do RF010 com rótulo próprio e sem deslocamento pelo fuso do navegador. O instante de auditoria conserva sua semântica de registro em UTC. A V16 acrescenta o local opcional aos recibos de ocorrência e interrupção. O campo entra na verificação de idempotência e aparece, junto da situação opcional da encomenda, apenas no acompanhamento protegido do RF010. O RF014 mantém sua projeção anterior. A asserção do RF014 verifica que o local e a situação da encomenda permanecem fora do histórico geral.

Passaram 47 testes, incluindo 4 de Selenium. Passaram o build e o ESLint da feature. As seis capturas foram renovadas com o novo campo habilitado. Foram encerrados 1.4, 2.4 e 6.2. O checklist chegou a **34 de 39**. As tarefas 2.1/2.3 e 4.5/5.4 aguardam, respectivamente, o contrato compartilhado do histórico e o consumo efetivo pelo RF013. A 8.2 aguarda o vínculo de motorista próprio e a designação suportados por RF002/RF009.
