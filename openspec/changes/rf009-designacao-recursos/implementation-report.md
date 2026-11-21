# Relatório de implementação do RF009

## Verificação inicial — 21/11/2026

- Branch local: `main`, revisão `b64aba5`, alinhada à `origin/main` após `git fetch --prune origin`.
- Worktrees: somente o diretório principal do projeto.
- Alterações locais preservadas: `.gitignore`, arquivos `package-lock.json`, lock temporário do documento e `docs/interface/designar-prestadores/`.
- Nenhuma alteração local preexistente foi descartada, restaurada ou sobrescrita.
- Migrações na `main`: V1, V2, V3, V4, V5, V6, V7 e V7.1.
- PR aberto #20, `feat/ConfirmacaoDeContratacao`, revisão `c2af490`: implementa o RF008 e adiciona `V8__rf008_frete_id.sql`.
- Arquivos protegidos pelo PR #20 que o RF009 não deve modificar antes de autorização/integração: `.gitignore`, `frontend/src/app/Router.tsx`, páginas e componentes compartilhados listados no diff do PR, além da migração V8 e dos arquivos próprios do RF008.
- O RF009 pode manter backend e frontend em arquivos próprios, mas a numeração de sua migração e a integração de rota dependem da decisão sobre o PR #20.

## Estado

A implementação foi retomada após a integração do PR #20 na `main` (`6cfa45b`). A branch `feat/rf009-designacao-recursos` parte dessa revisão e preserva as alterações locais anteriores.

## Contrato confirmado do RF008

- O aceite atualiza a proposta para `ACEITA` e a solicitação para `CONTRATADA`.
- O RF008 cria `frete` com `solicitacao_id`, `proposta_aceita_id`, `prestador_responsavel_id`, `valor_atual`, `condicoes_atuais` e status `AGUARDANDO_DESIGNACAO`.
- A migração V8 fornece a sequência do identificador de `frete`; a próxima versão disponível para o RF009 é V9.
- Fixture de pessoas: solicitação `907031`, modalidade `PESSOA`, três passageiros, valor `315.78` e condições `Inclui pedágios`.
- Fixture de encomenda: solicitação `907032`, modalidade `ENCOMENDA`, duas caixas, peso `5 kg`, valor `198.46` e condições `Inclui coleta`.
- `ContratacaoIntegrationTest` consulta as duas modalidades, cria e consulta frete em `AGUARDANDO_DESIGNACAO` e usa consultas SQL para confirmar unicidade, notificação e rollback. As inserções dos fixtures satisfazem as FKs do PostgreSQL usadas pelo teste.

## Referências de interface

- Foram conferidos os PNGs e JSONs locais “Prestador · Designar motorista e veículo” e “Prestador · Substituir recursos”.
- Os arquivos estruturais confirmam os campos, ações, estados e mensagens das duas telas do portal do prestador.
- Não existe frame local para consulta da designação pelo cliente. Conforme decisão aprovada, a etapa atual entrega somente a API autenticada do cliente, sem criar rota ou tela visual não especificada.
- Volume em m³ e conflito de agenda aparecem como exemplos visuais, mas não serão declarados como validados porque o modelo atual não contém os dados necessários.

## Arquivos compartilhados

- Após a integração do RF008, a API pública do GitHub não retornou PRs abertos.
- O usuário autorizou explicitamente a aplicação da mudança `rf009-designacao-recursos` após revisar o planejamento e integrar o RF008.
- O RF009 criará configuração de segurança e feature próprias. A edição de `frontend/src/app/Router.tsx` será aditiva e mínima, sem alterar rotas existentes.
- O cadastro/autenticação do autônomo e os arquivos reservados ao RF002 não serão modificados na Etapa 1.

## Migração V9

- Criada `V9__rf009_designacao_recursos.sql`, depois da V8 integrada pelo RF008.
- A migração adiciona `frete.revisao_designacao`, sequências de IDs para acordos e histórico, revisão de origem do acordo, estados consistentes e unicidade de acordo pendente por frete.
- Nenhuma tabela ou coluna do cadastro do autônomo foi alterada.
- Banco limpo `muv_rf009_clean`: o Spring/Flyway validou e aplicou 10 migrações até a V9; `BackendApplicationTests` passou com 1 teste, sem falhas.
- Banco controlado `muv_rf009_upgrade`: V1–V8 foram aplicadas, registros anteriores de frete, acordo e histórico foram inseridos, e a V9 preservou esses dados.
- Após a atualização, o frete anterior permaneceu `PRONTO_INICIO`, o acordo 800 permaneceu `PENDENTE` e as novas sequências avançaram para 801 e 701.

## Fixtures da organização

- Criados `rf009-designacao.sql` e `rf009-clean.sql` com organização, motoristas verificado/pendente/rejeitado, motorista externo e seis veículos cobrindo modalidade, atividade, propriedade e capacidade.
- Os fixtures incluem fretes em `AGUARDANDO_DESIGNACAO`, `PRONTO_INICIO`, `INTERROMPIDO`, `EM_EXECUCAO`, `CANCELADO` e um frete pertencente a outro prestador.
- A carga no PostgreSQL produziu 6 fretes, 4 vínculos, 6 veículos e 6 eventos sem violar FKs ou checks.
- A limpeza removeu todos os registros controlados; as contagens finais de frete, motorista, veículo e usuário foram zero.

## Fronteira temporária do autônomo

- O fixture inclui o prestador autônomo `909008`, com autorização e veículo próprios, mas deliberadamente sem registro em `motorista` e sem `vinculo_motorista`.
- O frete `909066` foi carregado em `AGUARDANDO_DESIGNACAO`, com `motorista_atual_id` e `veiculo_atual_id` nulos.
- A consulta de verificação retornou zero motoristas criados e zero vínculos criados para o autônomo; o carregamento não realizou designação automática.
- A limpeza posterior removeu integralmente os dados controlados, com zero usuários e fretes restantes no intervalo do RF009.

## Consulta de contexto da designação

- Criados DTOs próprios e repositório JDBC para carregar frete, solicitação, revisão de concorrência, designação atual e recursos pertencentes ao prestador responsável.
- A consulta de motoristas inclui somente vínculos `VERIFICADO`; a consulta de veículos limita os resultados pelo identificador do prestador.
- `DesignacaoRepositoryTest`: 3 testes aprovados no PostgreSQL isolado, incluindo frete sem designação, designação atual e exclusão explícita de recursos de outros prestadores.
- Uma primeira execução não iniciou o contexto porque foram usadas variáveis incompatíveis com o perfil `rf001-local`; a repetição com `MUV_LOCAL_DB_*` e a propriedade Selenium exigida pelo contexto concluiu com `BUILD SUCCESS`.

## Compatibilidade de veículos

- A regra considera somente propriedade, atividade, modalidade e capacidade verificável de passageiros ou carga em quilogramas.
- A modalidade `AMBOS` é aceita para pessoas e encomendas e capacidades iguais ao requisito são válidas.
- Volume, categoria de CNH e agenda não são inferidos.
- `CompatibilidadeDesignacaoServiceTest`: 7 testes unitários aprovados, sem falhas.

## Leitura autenticada do prestador

- Implementado `GET /api/prestador/fretes/{id}/designacao`, com o prestador derivado exclusivamente da identidade da sessão.
- Frete inexistente e frete pertencente a outro prestador retornam 404 sem revelar a existência do registro; sessão ausente retorna 401.
- O contrato inclui contexto, revisão, designação atual, motoristas verificados e veículos com incompatibilidades calculadas no servidor.
- `DesignacaoControllerTest`: 4 métodos MockMvc aprovados, cobrindo cinco cenários de acesso e ausência explícita de CPF, CNH, telefone, senha, habilitação cifrada e estado interno de verificação.

## Validação transacional da designação inicial

- O comando bloqueia o frete com `SELECT ... FOR UPDATE` e reconsulta vínculo verificado e veículo antes de persistir.
- Ausência de recurso, vínculo não verificado, veículo inexistente, alheio, inativo, de modalidade errada ou com capacidade insuficiente interrompem a operação antes da escrita.
- Estado e revisão desatualizados produzem conflito; o identificador do prestador continua vindo da sessão.
- `DesignacaoServiceTest`: 5 métodos unitários aprovados, incluindo todas as classes de incompatibilidade exigidas.

## Persistência da designação inicial

- A designação grava motorista e veículo, altera o estado para `PRONTO_INICIO`, incrementa a revisão e insere evento `DESIGNACAO` com o par completo.
- `DesignacaoIntegrationTest`: 2 testes aprovados no PostgreSQL, incluindo o estado final e rollback integral quando a inserção do histórico falha.
- A primeira simulação de falha foi corretamente recusada pelo PostgreSQL porque a restrição de teste atingia eventos preexistentes; o teste foi limitado ao frete controlado e então confirmou a atomicidade com `BUILD SUCCESS`.

## Endpoint da designação inicial

- O `POST /api/prestador/fretes/{id}/designacao` deriva o prestador da sessão e exige CSRF.
- `DesignacaoControllerTest`: 7 testes aprovados após a inclusão do POST, cobrindo sucesso organizacional, campos obrigatórios, acesso alheio, estado inelegível e revisão desatualizada.

## Fronteira operacional da etapa organizacional

- O autônomo legado sem motorista associado recebe 422 antes de qualquer tentativa de criar ou selecionar recursos.
- O teste MockMvc e as consultas SQL confirmaram que o frete autônomo permanece sem recursos e que não surgem motorista, vínculo ou histórico.
- O teste de integração também confirmou que uma organização não torna o frete pronto informando apenas um dos recursos.
- Execução conjunta: 16 testes aprovados entre unidade, MockMvc e PostgreSQL, sem falhas.

## Regra de substituição organizacional

- A substituição direta exige justificativa, revisão atual e frete em `PRONTO_INICIO` ou `INTERROMPIDO`.
- O par resultante sempre permanece completo, mesmo quando apenas motorista ou veículo é alterado; `INTERROMPIDO` não é promovido automaticamente.
- `EM_EXECUCAO` é rejeitado e requer interrupção pelo RF010 antes da troca.
- `SubstituicaoServiceTest`: 4 testes aprovados; execução unitária conjunta com a designação: 9 testes sem falhas.

## Atomicidade da substituição direta

- A atualização do par vigente, o evento `SUBSTITUICAO_SEM_ALTERACAO` e a notificação do cliente são executados na mesma transação.
- O histórico registra autor, data, justificativa e o par completo resultante.
- `SubstituicaoIntegrationTest`: 2 testes aprovados no PostgreSQL, incluindo rollback sem histórico ou notificação órfã quando a notificação falha.

## Endpoint de substituição

- Implementado `POST /api/prestador/fretes/{id}/substituicoes` com CSRF, sessão e revisão de concorrência.
- Os testes cobrem troca de um recurso, troca real dos dois recursos, incompatibilidade e segunda confirmação baseada em revisão antiga.
- A fixture passou a ter dois motoristas verificados para testar a troca completa; expectativas de consulta foram atualizadas sem ampliar dados de produção.
- Execução conjunta de controller e repositório: 13 testes aprovados.

## Solicitação de novo acordo

- Alterações de valor ou condições criam `solicitacao_alteracao_acordo` em `PENDENTE` com o par proposto completo e a revisão de origem.
- O frete mantém valor, condições, motorista, veículo e revisão anteriores enquanto o cliente não responde.
- O cliente recebe notificação na mesma transação.
- `SubstituicaoIntegrationTest` e testes unitários: 7 testes aprovados.

## Resposta do cliente ao novo acordo

- Implementado `POST /api/cliente/fretes/{freteId}/acordos/{acordoId}/resposta`, autenticado pela sessão do cliente responsável.
- O aceite revalida motorista, veículo, estado e revisão sob bloqueio; aplica valor, condições e recursos uma única vez e registra `SUBSTITUICAO_COM_ACORDO` ligado à solicitação aceita.
- A recusa preserva integralmente o frete. Respostas repetidas são idempotentes, cliente alheio recebe `404` e recurso que se tornou inelegível conserva o acordo pendente.
- A integração PostgreSQL verifica o estado final do frete, a ligação do histórico, a data da resposta e as notificações do cliente e do prestador.
- `AcordoControllerTest` e `SubstituicaoIntegrationTest`: 7 testes aprovados sem falhas.

## Revalidação da consulta e compatibilidade

- `DesignacaoRepositoryTest`, `CompatibilidadeDesignacaoServiceTest` e `DesignacaoControllerTest`: 20 testes aprovados sem falhas.
- A busca por identificadores e valores exclusivos das fixtures nos fontes de produção (`frontend/src` e `backend/src/main`) não encontrou dados demonstrativos fixos.

## Fundação da feature React

- Criados os tipos do contrato RF009, o cliente HTTP com sessão/CSRF e tratamento explícito de conflito, e estados puros de carregamento, vazio, erro e sucesso.
- O estado associa cada resposta ao identificador do frete consultado, descartando respostas atrasadas após a navegação para outro frete.
- `node --test test/designacao-state.test.mjs`: 2 cenários aprovados.
- A checagem TypeScript dos arquivos novos passou. A instalação local foi recomposta com `npm ci`, sem regravar o lockfile, e o build global passou.

## Portal do prestador

- Implementadas as telas de designação e substituição usando `ProviderLayout`, Tailwind, React Router e exclusivamente dados devolvidos pela API.
- Adicionadas as rotas `/prestador/fretes/:id/designacao` e `/prestador/fretes/:id/substituicao`, preservando as rotas existentes.
- O `ProviderLayout` recebeu somente classes responsivas: mantém a lateral no desktop e usa navegação horizontal compacta em viewport estreito, sem rolagem horizontal da página.
- A tela cobre carregamento, vazio, erro, conflito recuperável, confirmação, ausência de recursos, compatibilidade, justificativa obrigatória e trava contra envio duplicado.
- `npm run build` aprovado; lint dos arquivos novos, do layout e do roteador aprovado. O lint global ainda possui dois erros preexistentes em `features/driver-linking/index.tsx`.

## Teste sistêmico do prestador

- `DesignacaoSystemTest` executado contra Chrome headless, Vite real em `127.0.0.1:5173`, API Spring Boot em `8080` e PostgreSQL `muv_rf009_clean`.
- Cobertura: login, acesso direto, recurso incompatível, viewport 1440×1100 e 390×844, conflito de revisão recuperável nas duas telas, designação inicial e substituição válida.
- As consultas PostgreSQL confirmaram status, recursos, revisão, dois eventos de histórico e uma única notificação; 1 teste aprovado, sem falhas.
- Evidências visuais geradas em `backend/target/rf009-evidencias/` (artefatos locais de teste, não versionados).

## Contrato de consulta do cliente

- Endpoint: `GET /api/cliente/fretes/{freteId}/designacao`, autenticado e limitado ao passageiro que criou a solicitação do frete.
- Resposta para uma futura tela aprovada: `freteId`, `status`, `motoristaNome`, `veiculoMarca`, `veiculoModelo` e `veiculoPlaca`. Os quatro dados da designação são `null` enquanto ela não existir.
- O contrato não expõe identificadores internos dos recursos, CPF, CNH, telefone, senha, estado de verificação ou dados cifrados.
- `ClienteDesignacaoControllerTest`: 4 testes aprovados para frete próprio, sem designação, cliente alheio e sessão ausente, incluindo comparação da lista exata de campos serializados.
- A busca em `frontend/src` confirmou que não foi criada rota nem componente visual de cliente para o RF009; somente as duas telas autorizadas do prestador existem.

## Validação consolidada da etapa organizacional

- Frontend: `npm run build` aprovado; `node --test test/designacao-state.test.mjs test/trip-planning.test.mjs` aprovou 2 arquivos de teste sem falhas.
- Backend: suíte explícita de 9 classes (`CompatibilidadeDesignacaoServiceTest`, `DesignacaoRepositoryTest`, `DesignacaoServiceTest`, `DesignacaoIntegrationTest`, `DesignacaoControllerTest`, `SubstituicaoServiceTest`, `SubstituicaoIntegrationTest`, `AcordoControllerTest` e `ClienteDesignacaoControllerTest`) aprovou 43 testes sem falhas.
- Sistema: `DesignacaoSystemTest` aprovou 1 teste Selenium completo sem falhas, usando Vite, API e PostgreSQL reais.
- O PostgreSQL validou as 10 migrações disponíveis e manteve o esquema na versão 9 durante as execuções.

## Revisão de escopo e proteção do RF002

- Branch revisada: `feat/rf009-designacao-recursos`, baseada no commit `6cfa45b`; existe somente o worktree atual.
- Nenhum arquivo de cadastro/autenticação, `Prestador`, `AuthService`, `DriverAccountService`, `AuthController` ou `frontend/src/features/auth` foi modificado pelo RF009.
- `.gitignore`, o arquivo temporário de DOCX e os três `package-lock.json` não fazem parte da implementação do RF009; eram alterações/arquivos preexistentes e foram preservados. `npm ci` não gerou diff nos lockfiles.
- Os únicos arquivos compartilhados modificados pelo RF009 são `Router.tsx` e `ProviderLayout.tsx`, ambos após a autorização registrada na tarefa 1.4 e com alterações limitadas a duas rotas e responsividade.
- Esta é somente a etapa organizacional. As tarefas obrigatórias da integração com RF002 continuam abertas; portanto, o RF009 permanece parcial e não pode ser arquivado.
