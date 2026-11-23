# Prompts do RF010

Copie o bloco correspondente à etapa desejada. Use o primeiro após a aprovação da proposta e a autorização para implementar o RF010. Use o segundo quando houver uma implementação para revisar.

## 1. Implementar o RF010

```text
Implemente o RF010 (Acompanhamento da execução e registro de ocorrências) conforme a proposta aprovada. Antes de alterar código, leia integralmente AGENTS.md e os seguintes arquivos da mudança:

- openspec/changes/rf010-acompanhamento-execucao-ocorrencias/proposal.md
- openspec/changes/rf010-acompanhamento-execucao-ocorrencias/design.md
- openspec/changes/rf010-acompanhamento-execucao-ocorrencias/specs/acompanhamento-execucao-ocorrencias/spec.md
- openspec/changes/rf010-acompanhamento-execucao-ocorrencias/tasks.md

Use as referências anexadas conforme o fluxo de interface do projeto: 1000407677.jpg para registro de ocorrência e interrupção, 1000407679.jpg para retomada e 1000407681.jpg para encerramento sem conclusão. Se faltar uma referência, indique qual e avance nas tarefas autorizadas que independem dela.

Confira a branch, as mudanças locais e as integrações com RF009 e RF011. Valide a mudança com openspec validate rf010-acompanhamento-execucao-ocorrencias --strict. Descreva brevemente a ordem de execução. Antes de executar a parte afetada, leve para decisão qualquer divergência que altere os critérios de aceite. Respeite o escopo autorizado, inclusive a exigência de autorização explícita para alterações no backend. Preserve os artefatos da proposta, atualizando o checklist somente após verificar cada tarefa concluída.

Implemente as tarefas aprovadas do RF010 na ordem das dependências. Use o registro Open Questions de design.md para identificar o trabalho independente. Encaminhe mudanças em módulos, tabelas ou contratos de RF002, RF003, RF009, RF011, RF012, RF013 e RF014 aos responsáveis. Para avisos e consulta geral do histórico, siga os limites de RF013 (Rafael) e RF014 (Josué). Verifique que o início exige o motorista designado e os recursos válidos. Para ocorrência, interrupção, retomada e encerramento, autorize o prestador responsável ou o motorista atualmente designado para aquele frete. Aplique os estados de origem e destino da especificação, registre o autor autenticado e grave os fatos de execução conforme o contrato aprovado. Teste o reenvio autorizado após perda de resposta, a nova interrupção em frete já interrompido e a confirmação de um formulário de ciclo anterior após retomada. Cubra a rejeição de outro motorista da mesma empresa e de um motorista substituído. Nos formulários, mostre o efeito da ação antes da confirmação e preserve estado, histórico e notificações ao voltar sem confirmar. Use os dados reais do frete em lugar dos exemplos das capturas.

Execute as verificações previstas em tasks.md que correspondem às entregas realizadas, incluindo os testes de transições, autoria, concorrência e navegação. Ao final, informe o que foi entregue, arquivos alterados, comandos executados com seus resultados e tarefas pendentes ou bloqueadas.
```

## 2. Revisar a implementação do RF010

```text
Após a implementação, revise o RF010 (Acompanhamento da execução e registro de ocorrências) somente por leitura. Apresente correções sugeridas e aguarde autorização para editar código, artefatos da proposta ou checklist. Leia AGENTS.md e os seguintes arquivos da mudança:

- openspec/changes/rf010-acompanhamento-execucao-ocorrencias/proposal.md
- openspec/changes/rf010-acompanhamento-execucao-ocorrencias/design.md
- openspec/changes/rf010-acompanhamento-execucao-ocorrencias/specs/acompanhamento-execucao-ocorrencias/spec.md
- openspec/changes/rf010-acompanhamento-execucao-ocorrencias/tasks.md

Compare o diff e os testes com os requisitos e cenários da especificação. Confirme que cada tarefa marcada tem evidência. Use ambiente e dados de teste para exercitar os fluxos disponíveis.

Verifique o início apenas após designação válida e os pares de estados das quatro ações: ocorrência mantém EM_EXECUCAO, interrupção passa de EM_EXECUCAO para INTERROMPIDO, retomada volta de INTERROMPIDO para EM_EXECUCAO e encerramento passa de INTERROMPIDO para ENCERRADO_SEM_CONCLUSAO. Confirme a permissão do prestador responsável e do motorista atualmente designado e a rejeição de outro motorista da mesma empresa ou de um motorista substituído. Verifique autoria no histórico, descrição ou justificativa, horários, referência à interrupção e os fatos previstos para RF013, conforme o contrato aprovado. Confira os três cenários distintos de reenvio autorizado, nova interrupção em frete interrompido e formulário antigo após retomada. Use Open Questions de design.md para identificar o que ficou pendente e verifique os limites com RF009, RF011, RF013 e RF014. Confirme que o encerramento não registra uma conclusão ou entrega.

Compare as telas implementadas com 1000407677.jpg (registro de ocorrência e interrupção), 1000407679.jpg (retomada) e 1000407681.jpg (encerramento sem conclusão). Em desktop e em tela estreita, confira campos, avisos de efeito, ações de confirmação, estados exibidos e os botões de retorno. Exercite o retorno sem confirmar em cada tela e verifique que não há gravação de evento, mudança de estado ou notificação. Se faltar uma referência ou o ambiente impedir um teste, registre a limitação e faça as demais verificações.

Apresente primeiro os achados por gravidade, com arquivo e linha, cenário afetado, comportamento observado e esperado e correção sugerida. Depois indique, por requisito, o que foi atendido, não atendido ou não verificado, com a evidência disponível. Liste os comandos executados e seus resultados, as limitações e eventuais tarefas marcadas sem evidência. Trate a validação estrutural do OpenSpec como distinta da verificação dos fluxos da aplicação.
```
