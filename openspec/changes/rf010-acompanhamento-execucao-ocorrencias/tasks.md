# Tarefas do RF010

Esta lista acompanha a implementação de `specs/acompanhamento-execucao-ocorrencias/spec.md` conforme `design.md`. As evidências da entrega parcial e os bloqueios por tarefa estão no [relatório de implementação](implementation-report.md). Marcar uma tarefa somente após realizar e verificar seu resultado. Os IDs P1 a P7 apontam para as decisões e dependências registradas em [design.md — Open Questions](design.md#open-questions). Execute apenas o trabalho autorizado do RF010 e mantenha o aceite dependente em aberto enquanto o requisito responsável não liberar o contrato.

## 1. Preparação e contratos

- [x] 1.1 Antes de implementar, reler `AGENTS.md`, proposta, design e especificação e obter autorização para o escopo de implementação, inclusive backend; verificar que a autorização cobre RF010 e registrar os limites no relatório futuro.
- [x] 1.2 Conferir branch, status, worktrees, commits, PRs e versões Flyway; verificar os arquivos compartilhados livres ou protegidos e registrar a versão de migração disponível sem alterar trabalho alheio.
- [x] 1.3 Conferir sessão e contratos de leitura de RF003/RF009 e o bloqueio compartilhado com RF011. Verificar a matriz de estados com dados controlados. Cadastro, designação, substituição, cancelamento e conclusão são dos requisitos responsáveis.
- [ ] 1.4 Verificar que as referências visuais e as decisões que afetam a tela estão registradas conforme P2, P3 e P6. Comparar campos, confirmações e retornos antes de implementar as partes afetadas.
- [x] 1.5 Verificar a dependência do autônomo conforme P7 e registrar quais fluxos estão disponíveis para teste sem alterar RF002/RF009.

## 2. Persistência do contexto auditável

Requisitos: preservar histórico e autoria; validar dados temporais; revalidar confirmações repetidas.

- [ ] 2.1 Revisar o esquema de histórico, os dados legados e a leitura do RF014 com Josué conforme P5. Registrar o contrato aprovado e a responsabilidade por eventual mudança compartilhada. Encaminhar alterações de RF014 a Josué.
- [x] 2.2 Criar fixtures e limpeza exclusivas RF010 com pessoas, encomendas, empresa, motorista designado, colega da mesma empresa, motorista externo, contas e estados relevantes; verificar carga, FKs, limpeza e isolamento em banco de teste.
- [ ] 2.3 Implementar a gravação dos fatos RF010 e a leitura operacional conforme o contrato aprovado em P5. Verificar autoria, vínculo à interrupção do mesmo frete, estados anterior/resultante, referências de ciclo e recuperação do resultado original em reenvio.
- [ ] 2.4 Validar e persistir data e horário conforme P2. Verificar dados ausentes ou inválidos e, após a decisão, testar armazenamento e exibição esperados sem alterar a semântica dos registros anteriores.

## 3. Consulta e autorização por frete

Requisitos: autorizar ações pelo vínculo atual; consultar o acompanhamento do próprio frete.

- [x] 3.1 Implementar a resolução do prestador responsável ou da conta do motorista atual usando a sessão, sem aceitar autor/papel do payload; verificar com JUnit o prestador, motorista designado, colega verificado da mesma empresa, motorista substituído, cliente e prestador alheio.
- [x] 3.2 Implementar `GET /api/fretes/{freteId}/execucao` com estado exato, revisão, ciclo vigente ou interrupção em aberto, eventos necessários ao acompanhamento e ações elegíveis. Verificar com MockMvc sessão ausente, frete inexistente, vínculo indevido e ausência de CPF, CNH ou contatos desnecessários.
- [ ] 3.3 Definir DTOs e erros de validação/conflito dos comandos e proteção CSRF conforme a sessão existente; verificar contratos serializados e POST anônimo com CSRF válido, ausente e inválido, além da tentativa de falsificar autoria.
- [x] 3.4 Documentar a matriz de permissões e os contratos entregues no relatório da implementação; conferir que o vínculo com a organização não autoriza um motorista que não esteja designado.

## 4. Início, ocorrência e interrupção

Requisitos: registrar início; ocorrência sem interrupção; interrupção justificada; fatos de integração; limites do ciclo de vida.

- [ ] 4.1 Implementar a operação transacional com o bloqueio compartilhado com RF009/RF011. Revalidar a autorização no servidor e, para novos comandos, conferir estado, revisão e ciclo ou interrupção. Verificar toda a matriz de estados e o rollback de estado e evento.
- [ ] 4.2 Implementar início pelo motorista designado de `PRONTO_INICIO` para `EM_EXECUCAO`, revalidando recursos do RF009; verificar em serviço/API o sucesso e o bloqueio sem recursos válidos, por outro motorista ou em estado inelegível, com `INICIO` e autoria correta no banco.
- [ ] 4.3 Implementar ocorrência por prestador ou motorista designado mantendo `EM_EXECUCAO`; verificar nas duas modalidades descrição/data/horário obrigatórios, espaços Unicode, horário inválido, estados anterior/resultante iguais e histórico sem interrupção automática.
- [x] 4.4 Implementar interrupção por cada ator permitido com justificativa obrigatória. Verificar autoria, transição para `INTERROMPIDO`, preservação de recursos e recusa de uma nova operação quando já interrompido.
- [ ] 4.5 Identificar o fato único de interrupção disponível para o RF013 e registrar o alinhamento P4 com Rafael para os três avisos dos frames. Acompanhar eventual alteração de contrato ou tabela pelo RF013.
- [ ] 4.6 Testar reenvio autorizado após perda da resposta, inclusive após a primeira interrupção mudar o estado. Verificar resposta original e ausência de novo evento, transição ou efeito de integração. Cobrir chave reutilizada com outro conteúdo e duas ocorrências independentes no mesmo ciclo.

## 5. Retomada e encerramento sem conclusão

Requisitos: retomar frete interrompido; encerrar sem conclusão; preservar histórico; revalidar contexto.

- [x] 5.1 Implementar retomada por prestador ou motorista atual, com justificativa, data/horário, confirmação de prosseguimento, recursos revalidados e referência à interrupção; verificar `INTERROMPIDO` para `EM_EXECUCAO`, `RETOMADA` e preservação do evento original.
- [x] 5.2 Implementar encerramento por ambos os autores permitidos, com motivo, data/horário e confirmação de não conclusão; verificar `INTERROMPIDO` para `ENCERRADO_SEM_CONCLUSAO`, vínculo à interrupção e ausência de entrega, campos de conclusão, cancelamento e mudanças financeiras.
- [ ] 5.3 Rejeitar justificativas vazias, confirmações ausentes, estados incompatíveis e comandos em estados terminais; verificar JUnit/MockMvc e consultas SQL sem alteração de estado, histórico ou notificações após cada rejeição.
- [ ] 5.4 Verificar que retomada e encerramento geram um único fato de execução cada e que o reenvio não repete seus efeitos. Após P4, verificar o contrato acordado com o RF013, deixando entrega e política de avisos no requisito responsável.
- [ ] 5.5 Testar troca de motorista e ciclos sucessivos. Verificar recusa do motorista substituído e de formulários antigos de retomada ou encerramento. Abrir ocorrência e interrupção, interromper e retomar com a mesma designação e verificar que os formulários anteriores pedem atualização mesmo em `EM_EXECUCAO`.
- [ ] 5.6 Executar testes de concorrência PostgreSQL para início versus cancelamento nos dois sentidos e retomada versus encerramento; verificar que só a transição válida vence e que não há evento órfão, retorno pré-execução ou sobrescrita da ação vencedora.

## 6. Interface de registro e interrupção

Requisitos: consultar acompanhamento; confirmar efeito e permitir voltar sem registrar; registrar ocorrência/interrupção.

- [ ] 6.1 Criar a feature React com tipos, cliente HTTP/CSRF e estados de carregamento, erro, conflito e envio; verificar que trocar de frete limpa o contexto anterior e que a interface usa IDs e dados reais, sem valores demonstrativos das imagens.
- [ ] 6.2 Implementar o formulário e o painel de efeito conforme `1000407677.jpg`. Verificar tipo, data e horário obrigatórios, descrição ou justificativa, recursos atuais e distinção visual entre manter execução e interromper. Aplicar P2/P3 aos dados e P6 à verificação visual.
- [ ] 6.3 Integrar consulta e ação de início no contexto aprovado do frete e limitar opções por estado/papel; verificar que não há início sem designação, autorização por mera visibilidade do botão ou quarta tela completa inventada.
- [ ] 6.4 Implementar confirmação e “Voltar sem registrar”, com retorno sem POST, bloqueio de duplo envio e apresentação do resultado persistido; verificar no navegador e no banco que ocorrência mantém `EM_EXECUCAO`, interrupção gera `INTERROMPIDO` e abandonar cada formulário não grava eventos/notificações.
- [ ] 6.5 Documentar os pontos de entrada e integrar somente rotas/arquivos compartilhados autorizados; verificar acesso direto, retorno ao frete correto, preservação das rotas existentes e que o motorista não recebe funções administrativas pelo layout.

## 7. Interface de retomada e encerramento

Requisitos: retomada; encerramento; confirmação e retorno sem escrita; autoria no histórico.

- [ ] 7.1 Implementar `1000407679.jpg` com interrupção em aberto, motivo/autor, recursos atuais, campos obrigatórios e checkbox; verificar desktop/viewport estreito e cenário Selenium de retomada confirmada com estado/evento/autor persistidos.
- [x] 7.2 Implementar `1000407681.jpg` com aviso de não conclusão, motivo, data e horário, autor autenticado e caixa de confirmação. Tratar o campo de encomenda conforme P3 e testar que o encerramento não grava entrega, recebedor ou conclusão.
- [x] 7.3 Implementar “Voltar ao acompanhamento” e “Voltar e manter interrompido” sem submit; verificar em Selenium e SQL que ambos mantêm `INTERROMPIDO` e a interrupção original, sem evento ou notificação novos.
- [ ] 7.4 Tratar conflito por troca de motorista/ciclo e falha de consulta após sucesso; verificar que textos são preservados para revisão, que uma nova ação exige confirmação e que sucesso já persistido não aparece como falha de gravação.
- [ ] 7.5 Conferir navegação por teclado, foco, mensagens e cartões empilhados das três telas; registrar capturas e resultado da comparação visual com as referências no relatório futuro.

## 8. Integração e revisão final da implementação

- [ ] 8.1 Executar nas duas modalidades os fluxos de contratação, designação, início, ocorrência e interrupção, com retomada e encerramento em caminhos separados. Verificar recarga, autores distintos e compatibilidade dos registros no histórico atual. Encaminhar mudanças de consulta, filtros ou apresentação ao RF014 conforme P5.
- [ ] 8.2 Executar o fluxo do autônomo após a liberação da dependência P7. Verificar designação, início, ações posteriores e autoria conforme P1. Manter o aceite desse cenário pendente enquanto RF002/RF009 não o suportarem.
- [ ] 8.3 Executar testes JUnit/MockMvc/PostgreSQL/Selenium da feature e regressões pertinentes RF009/RF011/RF014, testes Node relacionados, `npm run build` e lint dos arquivos alterados; registrar comandos, resultados reais e limitações, sem atribuir aprovação a teste não executado.
- [x] 8.4 Revisar diff, escopo autorizado e checklist contra todos os cenários; executar `openspec validate rf010-acompanhamento-execucao-ocorrencias --strict` e `git diff --check`, registrando implementação, evidências e pendências no relatório futuro.

## Workflow follow-up

- Após implementação autorizada, revisão e verificação de todas as tarefas obrigatórias, sincronizar a especificação principal e arquivar pelo fluxo OpenSpec.
- Não arquivar esta proposta de documentação como funcionalidade concluída nem marcar tarefas pela simples validação estrutural dos artefatos.
