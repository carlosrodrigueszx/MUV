# Tarefas do RF009

## 1. Proteção do trabalho compartilhado e contratos

- [x] 1.1 Conferir branch, `git status`, worktrees, commits, PRs pendentes e versões Flyway antes de editar; registrar no relatório da mudança os arquivos compartilhados livres ou protegidos e verificar que nenhuma alteração local de terceiro foi sobrescrita.
- [x] 1.2 Confirmar com o responsável pelo RF008 o contrato que cria o frete em `AGUARDANDO_DESIGNACAO`, com proposta, prestador, valor e condições; documentar um fixture de pessoas e outro de encomenda e verificar por consulta SQL que ambos satisfazem as FKs atuais.
- [x] 1.3 Conferir os PNGs e JSONs de “Designar motorista e veículo” e “Substituir recursos” e registrar que a consulta do cliente será API-only nesta mudança; verificar que nenhuma tela de cliente sem frame foi adicionada.
- [x] 1.4 Antes de modificar roteamento, segurança global ou outro arquivo envolvido em PR de colega, apresentar o arquivo e o impacto ao responsável e obter autorização; registrar a decisão no relatório de implementação.

## 2. Etapa 1 — Persistência e dados controlados

- [x] 2.1 Escolher a próxima versão Flyway após sincronizar a base e criar somente a migração aditiva necessária à revisão da designação e aos identificadores/estados do acordo, sem alterar o cadastro do autônomo; verificar a migração em PostgreSQL limpo e em cópia controlada do esquema atual.
- [x] 2.2 Criar fixtures exclusivas do RF009 para organização, motoristas verificado/pendente/rejeitado, veículos compatíveis/incompatíveis e fretes em cada estado relevante; verificar carregamento e limpeza no perfil de teste do PostgreSQL.
- [x] 2.3 Incluir fixture de autônomo ainda sem motorista associado apenas para validar a fronteira temporária; verificar que nenhuma consulta ou comando cria motorista, vínculo ou designação automaticamente.

## 3. Etapa 1 — Consulta de contexto e compatibilidade

- [x] 3.1 Criar DTOs e repositório JDBC para carregar frete, solicitação, designação atual, recursos do prestador e revisão de concorrência; verificar com teste de repositório que recursos de outro prestador nunca aparecem.
- [x] 3.2 Implementar regras de compatibilidade para modalidade, atividade, propriedade, passageiros e peso, sem inferir volume, categoria veicular ou agenda; verificar com testes unitários as duas modalidades, `AMBOS`, limites exatos e capacidade insuficiente.
- [x] 3.3 Implementar a leitura autenticada do prestador em `GET /api/prestador/fretes/{id}/designacao`; verificar com MockMvc sucesso, sessão ausente, prestador diferente, frete inexistente e resposta sem CPF, CNH, telefone ou dados cifrados.
- [x] 3.4 Executar os testes de consulta e compatibilidade com Maven e confirmar que nenhum dado fixo dos frames aparece nas respostas ou no frontend.

## 4. Etapa 1 — Designação inicial para organizações

- [x] 4.1 Implementar o comando transacional de designação com bloqueio do frete e revalidação de motorista e veículo; verificar com teste unitário ausência de recursos, vínculo não verificado, veículo alheio/inativo, modalidade errada e capacidade insuficiente.
- [x] 4.2 Persistir motorista e veículo atuais, incrementar a revisão, mudar `AGUARDANDO_DESIGNACAO` para `PRONTO_INICIO` e inserir `DESIGNACAO` com o par completo; verificar em teste de integração que as gravações são atômicas e sofrem rollback em falha parcial.
- [x] 4.3 Expor `POST /api/prestador/fretes/{id}/designacao` derivando o prestador da sessão; verificar com MockMvc sucesso organizacional, validação, acesso alheio, estado inelegível e conflito por revisão desatualizada.
- [x] 4.4 Enquanto o RF002 não estiver integrado, rejeitar explicitamente a designação de autônomo sem motorista associado e sem realizar escrita; verificar por MockMvc e consulta SQL a resposta de domínio e a ausência de alterações em frete, motorista, vínculo e histórico.
- [x] 4.5 Verificar com teste de integração que frete sem motorista ou veículo permanece bloqueado e que somente a designação organizacional completa resulta em `PRONTO_INICIO`.

## 5. Etapa 1 — Substituição organizacional e novo acordo

- [x] 5.1 Implementar substituição justificada em `PRONTO_INICIO` e `INTERROMPIDO`, gravando o par resultante completo e preservando o estado de interrupção quando aplicável; verificar com testes unitários os estados permitidos, justificativa vazia e rejeição em `EM_EXECUCAO`.
- [x] 5.2 Registrar `SUBSTITUICAO_SEM_ALTERACAO` e notificação do cliente na mesma transação; verificar por teste de integração designação anterior/resultante, autor, data, justificativa, destinatário e rollback sem notificação órfã.
- [x] 5.3 Expor `POST /api/prestador/fretes/{id}/substituicoes` com revisão de concorrência; verificar com MockMvc substituição de um recurso, dos dois recursos, incompatibilidade e conflito entre duas confirmações.
- [x] 5.4 Implementar solicitação pendente quando a troca organizacional altera valor ou condições, sem modificar a designação vigente; verificar em integração que o cliente é notificado e o frete conserva os dados anteriores.
- [x] 5.5 Implementar resposta autenticada do cliente ao novo acordo com aplicação idempotente no aceite e preservação no indeferimento; verificar aceite, rejeição, resposta repetida, cliente alheio e recurso que se tornou inelegível antes do aceite.
- [x] 5.6 No aceite válido, atualizar condições e recursos e registrar `SUBSTITUICAO_COM_ACORDO`; verificar em PostgreSQL a ligação com a solicitação aceita e a consistência das notificações aos envolvidos.

## 6. Etapa 1 — Portal do prestador

- [x] 6.1 Criar a feature React do RF009 com cliente de API, tipos e estados de carregamento, vazio, erro, conflito e confirmação; verificar com testes do frontend que a resposta de uma consulta não permanece ao abrir outro frete.
- [x] 6.2 Implementar a tela de designação com `ProviderLayout`, recursos retornados pela API e validações compatíveis com o frame; verificar visualmente em desktop e viewport estreito sem introduzir dados demonstrativos fixos.
- [x] 6.3 Implementar a tela de substituição com designação atual, novos recursos, justificativa, aviso de notificação e ações de confirmar/manter; verificar navegação, campo obrigatório, erro recuperável e bloqueio de envio duplicado.
- [x] 6.4 Integrar as rotas somente após a autorização da tarefa 1.4 e verificar acesso direto, retorno ao frete e manutenção dos itens existentes do roteador e da navegação compartilhada.
- [x] 6.5 Criar teste Selenium do prestador contra Vite, API e PostgreSQL reais cobrindo designação inicial, recurso incompatível, conflito desatualizado e substituição válida; verificar no banco o estado, histórico e notificação de cada cenário.

## 7. Etapa 1 — Consulta do cliente pela API

- [x] 7.1 Implementar `GET /api/cliente/fretes/{id}/designacao` limitado ao passageiro da solicitação e sem criar tela de cliente; verificar com MockMvc frete próprio, frete alheio, sessão ausente e frete ainda sem designação.
- [x] 7.2 Definir uma resposta mínima com nome do motorista e marca, modelo e placa do veículo, excluindo CPF, CNH, telefone e verificações internas; verificar o contrato serializado e um teste de privacidade por lista explícita de campos ausentes.
- [x] 7.3 Documentar o contrato que uma futura tela aprovada deverá consumir e verificar que o build atual não contém rota ou componente visual de cliente inventado para o RF009.

## 8. Validação da etapa organizacional

- [x] 8.1 Executar `npm run build` no frontend e os testes unitários, MockMvc, integração e Selenium da etapa 1 no backend com PostgreSQL de teste; registrar comandos, contagem e resultados reais no relatório da mudança.
- [x] 8.2 Revisar o diff da etapa 1 e confirmar que cadastro/autenticação do autônomo e demais arquivos do RF002 não foram modificados; registrar explicitamente que o RF009 continua parcial e não pode ser arquivado.
- [ ] 8.3 Publicar ou integrar a etapa organizacional somente após revisão da equipe e verificar novamente que todas as tarefas posteriores permanecem desmarcadas no OpenSpec.

## 9. Etapa 2 — Integração obrigatória com o RF002

- [ ] 9.1 Quando o RF002 iniciar, definir e documentar nele a relação definitiva entre `prestador_autonomo`, conta e `motorista`, incluindo estratégia para cadastros novos e legados; verificar a decisão contra as FKs de `frete` e `vinculo_motorista`.
- [ ] 9.2 Após autorização sobre os arquivos do RF002, criar migração aditiva específica para a relação autônomo–motorista sem renumerar migrações já aplicadas; verificar em PostgreSQL limpo e em cópia controlada do banco da etapa 1.
- [ ] 9.3 Implementar no RF002 a criação transacional do motorista próprio e do vínculo necessário para novos autônomos; verificar que conta, prestador, motorista e vínculo são gravados juntos ou sofrem rollback juntos.
- [ ] 9.4 Implementar no RF002 o tratamento idempotente dos autônomos legados sem registrar dados sensíveis em logs; verificar que duas execuções produzem um único motorista e um único vínculo.
- [ ] 9.5 Substituir a rejeição temporária do RF009 pela resolução do motorista autônomo fornecida pelo RF002; verificar que o restante do serviço, dos estados e dos contratos organizacionais permanece inalterado.
- [ ] 9.6 Cobrir a designação do autônomo recebendo apenas o veículo; verificar em unitário, MockMvc e PostgreSQL que o frete referencia o motorista próprio, o veículo elegível e o vínculo válido.
- [ ] 9.7 Cobrir a troca do profissional autônomo por novo acordo com o cliente; verificar que não há alteração antes do aceite e que o aceite válido produz `SUBSTITUICAO_COM_ACORDO` e notificações consistentes.
- [ ] 9.8 Executar o fluxo Selenium do autônomo contra Vite, API e PostgreSQL reais e registrar evidências da designação, bloqueio sem veículo e tratamento do novo acordo.

## 10. Integrações finais e encerramento

- [ ] 10.1 Quando o RF008 estiver disponível, executar o fluxo proposta aceita → frete aguardando designação → designação válida para organização e autônomo; verificar que o contrato não exige ajuste manual de dados.
- [ ] 10.2 Quando o RF010 estiver disponível, verificar que o início falha sem recursos, funciona em `PRONTO_INICIO` para o responsável autorizado e exige interrupção antes de substituir durante execução.
- [ ] 10.3 Revisar o diff completo, confirmar que somente arquivos necessários e autorizados foram alterados e anexar ao relatório endpoints, migrações, testes, limitações e pendências visuais.
- [ ] 10.4 Confirmar que a integração do RF002 e os testes RF008/RF010 estão concluídos antes de declarar o RF009 completo; verificar que nenhuma tarefa obrigatória permanece desmarcada.

## Workflow follow-up

- Após revisão da equipe e conclusão comprovada das duas etapas, sincronizar a especificação e arquivar a mudança pelo fluxo OpenSpec.
- Após o arquivamento, validar a especificação principal `designacao-motoristas-veiculos` e confirmar que os artefatos históricos permanecem disponíveis.
