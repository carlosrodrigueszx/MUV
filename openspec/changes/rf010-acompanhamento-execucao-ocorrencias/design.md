# Design técnico do RF010

## Context

Veja `proposal.md` para a motivação e `specs/acompanhamento-execucao-ocorrencias/spec.md` para os comportamentos verificáveis. A branch implementa a consulta, a interrupção, a retomada e o encerramento sem conclusão. As demais ações seguem planejadas.

Fontes consultadas e seu uso:

- `Documento - Figma, Modelos e Arquitetura do Software.pdf`, fornecido pelo usuário: RF010, US-012, RN011 a RN017, modelo de frete/histórico e fluxo de atividades, especialmente páginas 13, 18, 19, 24, 25 e 33.
- `1000407660.PDF`: transcrição usada como exemplo de separação entre requisitos, decisões e tarefas verificáveis. Seus prompts de implementação e o exemplo de aplicação climática não são instruções para executar código nesta etapa.
- `1000407677.jpg`, `1000407679.jpg` e `1000407681.jpg`: referências de registro/interrupção, retomada e encerramento, respectivamente. Os JPEGs aprovados estão em [evidencias/referencias/](evidencias/referencias/).
- `openspec/config.yaml`: `schema: spec-driven`, sem regras adicionais ativas. O schema e os precedentes RF009/RF011 usam proposta, design, delta de capacidade, tarefas e `.openspec.yaml`; não é necessário um `requirements.md` paralelo.
- Mudanças `rf009-designacao-recursos` e `rf011-cancelamento-fretes`: proposta, design, especificação, tarefas, metadados e relatórios de implementação. Seus relatórios são contexto histórico; a situação atual foi conferida no código.

A base atual contém React/TypeScript/Vite, `ProviderLayout`, sessão do RF001, rotas por identificador real, Spring Boot, PostgreSQL, Flyway e repositórios `JdbcClient` para fretes. `Frete` já contém os sete estados do ciclo de vida, motorista e veículo atuais e dados específicos de conclusão. `Historico_Execucao_Frete` já prevê os cinco tipos de evento RF010 e `usuario_autor_id`. `Motorista.usuario_id` associa a conta ao executor; vínculo com uma empresa não equivale à designação para um frete.

RF009 mantém `revisao_designacao`, designação completa e substituição somente em `PRONTO_INICIO` ou `INTERROMPIDO`. RF011 bloqueia a linha do frete, revalida o estado e usa atualização condicional: só cancela `AGUARDANDO_DESIGNACAO` ou `PRONTO_INICIO`. Ambos exigem que RF010 use a mesma fronteira de concorrência. A migração V10 já existe para RF015; não reservar V10 nem presumir a próxima versão livre.

A revisão da proposta ocorre na branch `feat/rf010-acompanhamento-ocorrencias`. A implementação e as integrações dependem da aprovação do escopo desta mudança.

## Goals / Non-Goals

**Goals:**

- Resolver permissão por frete e identidade real da sessão, inclusive quando o autor difere do motorista designado.
- Manter estado e evento consistentes e preservar a relação entre interrupção e seu desfecho.
- Reutilizar o modelo, as transações e os componentes existentes, com alterações aditivas e isoladas na futura implementação.
- Fornecer tarefas rastreáveis aos requisitos, com verificação de segurança, estados e interface junto de cada entrega.

**Non-Goals:**

- Implementar código, migrations, testes da aplicação ou novos arquivos fora desta mudança agora.
- Implementar RF011, RF012, RF013 completo, substituição/novo acordo do RF009 ou o cadastro autônomo do RF002.
- Criar perfil genérico de funcionário/gestor, subtipo de cooperativa ou autorização para todos os motoristas da empresa.
- Exigir comprovantes, fotografias, assinaturas, identificação de recebedor, multas, reembolsos ou integrações externas.

## Decisions

### 1. Autorização por conta e designação vigente

Para as quatro ações solicitadas, autorizar quando `sessao.id == frete.prestador_responsavel_id` ou quando a conta estiver associada ao `motorista_atual_id` daquele frete. Revalidar essa relação depois de obter o bloqueio, sem confiar em autor, papel, empresa ou motorista enviados pelo navegador. A conta do prestador empresa representa o gestor responsável no modelo existente; não criar delegações a funcionários que o cadastro não representa.

O início segue o critério explícito da US-012: motorista atualmente designado. Um autônomo pode iniciar como motorista próprio quando RF002/RF009 fornecerem a associação definitiva; ser apenas prestador não concede esse papel de início. As quatro ações de ocorrência/interrupção/retomada/encerramento aceitam também o prestador responsável, conforme o pedido desta mudança.

Na auditoria, usar `usuario_autor_id` da sessão. Para contas distintas, o papel corresponde ao vínculo autorizado. A apresentação quando a conta reúne ambos os vínculos segue P1. Consultar o vínculo e a designação através de RF003/RF009. A escrita de cadastro, vínculo, designação e substituição pertence aos módulos de origem.

### 2. Máquina de estados explícita

| Ação | Estado anterior exigido | Estado resultante | Evento | Autor permitido |
| --- | --- | --- | --- | --- |
| Iniciar | `PRONTO_INICIO` | `EM_EXECUCAO` | `INICIO` | Motorista designado |
| Registrar ocorrência sem interrupção | `EM_EXECUCAO` | `EM_EXECUCAO` | `OCORRENCIA` | Prestador responsável ou motorista designado |
| Interromper | `EM_EXECUCAO` | `INTERROMPIDO` | `INTERRUPCAO` | Prestador responsável ou motorista designado |
| Retomar | `INTERROMPIDO` | `EM_EXECUCAO` | `RETOMADA` | Prestador responsável ou motorista designado |
| Encerrar sem conclusão | `INTERROMPIDO` | `ENCERRADO_SEM_CONCLUSAO` | `ENCERRAMENTO_SEM_CONCLUSAO` | Prestador responsável ou motorista designado |

Qualquer combinação diferente é rejeitada sem escrita. Em `AGUARDANDO_DESIGNACAO`, o usuário deve completar RF009. `CANCELADO`, `CONCLUIDO` e `ENCERRADO_SEM_CONCLUSAO` não aceitam novos comandos RF010. Interromper não cancela; encerrar sem conclusão não preenche os campos de conclusão, destinatário ou entrega. Valor e condições permanecem iguais em todas as ações.

Retomada ou encerramento referencia o evento da interrupção em aberto. Cada início ou retomada abre um novo ciclo de execução. Um formulário de ocorrência ou interrupção usa como referência o ID do `INICIO` ou `RETOMADA` que abriu o ciclo vigente. Retomada e encerramento usam o ID da interrupção em aberto. Estado e revisão da designação podem permanecer iguais entre ciclos, então a confirmação de nova operação também compara o ID correspondente.

### 3. API de execução compartilhada pelos autores autorizados

A API do RF010 usa `/api/fretes/{freteId}/execucao`. A escrita disponível é `POST /interrupcoes`, `POST /retomadas` e `POST /encerramento-sem-conclusao`.

- `GET` retorna resumo, estado exato, designação, `revisaoDesignacao`, referência ao ciclo vigente ou à interrupção em aberto, eventos necessários ao acompanhamento e ações permitidas à sessão.
- `POST /interrupcoes` exige sessão e CSRF no padrão RF001/RF011. Recebe `chaveOperacao` (UUID), `estadoEsperado`, `revisaoDesignacao`, `cicloExecucaoId`, `justificativa`, `data` e `horario`. Devolve 200 com `eventoId`, `autorId`, `estadoAnterior` e `estadoResultante`, inclusive em reenvio. Os POSTs de início e ocorrência ficam para as próximas etapas.
- `POST /retomadas` usa a mesma sessão e CSRF. Recebe a mesma base de dados, com `interrupcaoId` no lugar de `cicloExecucaoId` e `confirmou: true`. Devolve a mesma estrutura, com estados `INTERROMPIDO` e `EM_EXECUCAO`.
- `POST /encerramento-sem-conclusao` usa sessão, CSRF, `interrupcaoId` e `confirmou: true` para declarar que o frete não foi concluído. Em frete de encomenda aceita `situacaoEncomenda` opcional. Devolve o resultado com os estados `INTERROMPIDO` e `ENCERRADO_SEM_CONCLUSAO`.
- Os comandos carregam `estadoEsperado`, `revisaoDesignacao` e uma `chaveOperacao` por confirmação; ocorrência e interrupção incluem `cicloExecucaoId`, enquanto retomada e encerramento incluem `interrupcaoId`. Descrição é obrigatória na ocorrência; justificativa é obrigatória na interrupção, retomada e encerramento. Retomada e encerramento recebem também suas confirmações explícitas.
- Retornar resultado persistido com ID do evento, estado e autoria. Um reenvio autorizado com a mesma chave, conta, ação e conteúdo devolve o resultado original, inclusive o estado resultante daquela confirmação. A leitura do contexto continua mostrando o estado vigente. Reutilizar a chave com dados diferentes produz conflito.
- Erros: 400 para dados inválidos; 401 para ausência de sessão; 403 para autor não autorizado ou CSRF inválido; 404 para frete inexistente; 409 para contexto/estado incompatível. Como em RF011, o filtro CSRF pode rejeitar POST anônimo antes da verificação de sessão. Não incluir dados protegidos na resposta de rejeição.

A leitura não reutiliza o rótulo agregado “Contratado” do RF014 para decidir ações: é necessário o estado real de `Frete`. Nenhuma permissão do backend depende da visibilidade de botões. A API comum evita duplicar lógica para prestador e motorista; não transforma a rota existente de cancelamento em endpoint de execução.

### 4. Transação, designação e concorrência com RF009/RF011

Usar serviço `@Transactional` e repositório JDBC, sem introduzir uma entidade JPA parcial de frete. A operação:

1. Em `READ COMMITTED`, adquire `SELECT ... FOR UPDATE OF f` na mesma linha de `frete` usada por RF009/RF011. Relê o estado e a conta do motorista atual depois de adquirir o bloqueio.
2. Revalida a autorização atual. Se a mesma conta ainda autorizada reenviar a mesma chave, ação e conteúdo, devolve o resultado original antes de verificar a elegibilidade do estado para uma nova operação. Uma chave reaproveitada com dados diferentes recebe conflito.
3. Para uma nova operação, confere estado, revisão e ciclo de execução ou interrupção referenciada. Uma nova interrupção em `INTERROMPIDO` recebe conflito. No início e na retomada, revalida recursos conforme RF009, sem inventar regras de agenda, volume ou categoria não representadas no cadastro.
4. Para transições, realiza UPDATE condicional ao estado anterior; para ocorrência, mantém o estado e acrescenta apenas o evento. Grava o contexto auditável de ambas as situações.
5. Insere o evento `INTERRUPCAO`, `RETOMADA` ou `ENCERRAMENTO_SEM_CONCLUSAO`, obtém o ID com `RETURNING id` e grava o recibo na mesma transação. Uma falha em qualquer gravação reverte estado, evento e recibo. A entrega de avisos segue P4.

O bloqueio também é necessário na ocorrência, apesar de ela não alterar o estado: impede aceitar um registro com base em execução já encerrada. Duas ocorrências independentes válidas podem ser registradas; o reenvio da mesma confirmação não produz outra. A chave da confirmação permite recuperar a resposta após timeout sem repetir a gravação do evento ou qualquer efeito de integração. O bloqueio de duplo clique é uma proteção adicional da interface.

Se o cancelamento vencer a disputa com o início, o início recebe conflito; se o início vencer, RF011 rejeita o cancelamento. Não retornar a estado pré-execução. Uma substituição durante interrupção exige recarregar a revisão antes da retomada; o motorista substituído perde a autorização de comando. Retomada e encerramento simultâneos resolvem o mesmo ciclo uma única vez.

### 5. Histórico, tempo e evolução aditiva dos dados

Reutilizar `historico_execucao_frete`, seus tipos de evento, `usuario_autor_id`, `descricao` e `justificativa`. Acrescentar os eventos RF010 de forma imutável, com autoria, estados anterior/resultante e vínculo do desfecho à interrupção original. As decisões P1 e P2 determinam quais dados de papel e horário precisam ser persistidos ou exibidos.

A V11 já aplicada permanece com o mesmo conteúdo e preserva `rf010_recibo_interrupcao`. A migração aditiva `V12__rf010_recibo_retomada.sql` é a próxima versão disponível na branch. Conferir a numeração com a equipe antes da integração. A tabela `rf010_recibo_retomada` guarda a chave por frete, o evento da retomada, a referência única à interrupção em aberto, a revisão, os estados e a data/hora informadas. A referência é validada no próprio frete sob bloqueio e gravada na transação do evento. O evento original permanece no histórico.

A V13 aditiva, `V13__rf010_recibo_encerramento.sql`, cria `rf010_recibo_encerramento` com referência à interrupção em aberto, chave por frete, evento único, revisão, estados, data/hora informada e `situacao_encomenda` opcional. É a próxima versão livre nesta branch e deve ser conferida antes da integração. V11 e V12 mantêm as definições e os recibos. A situação da encomenda só é recebida para a modalidade de encomenda. Fica no recibo próprio, não em `descricao` nem em `justificativa` do histórico. A decisão sobre sua visibilidade no histórico geral permanece em P3.

O serviço de transição e a busca de recibos são comuns às três ações. A busca consulta as três tabelas sob o mesmo bloqueio do frete. A mesma chave não pode ser reutilizada para outra ação. A autoria e a justificativa vêm do evento, e os demais dados vêm do recibo. Retomada e encerramento exigem confirmação explícita também no reenvio. Quando disputam a mesma interrupção, cada nova operação revalida a referência depois do bloqueio. Só a vencedora grava evento e recibo. O evento de retomada abre um novo ciclo, mesmo com a mesma designação.

Nas três ações, `historico_execucao_frete.data_hora` registra o horário do servidor, com `timezone('UTC', clock_timestamp())` como no RF011. O RF010 guarda o valor civil digitado no recibo. No acompanhamento, eventos com recibo próprio podem mostrar o horário de registro em UTC. Eventos legados sem recibo continuam sem atribuição de fuso pelo RF010. A interpretação, o fuso e a exibição do horário informado seguem P2. O local da ocorrência segue P3.

As tabelas existentes e o contrato de leitura e ordenação do RF014 permanecem intactos. Fretes com dados legados sem evento inicial ou de retomada coerente ficam consultáveis, com as ações que exigem referência de ciclo indisponíveis. P5 continua relevante para eventuais mudanças compartilhadas.

A consulta RF010 é operacional e vinculada ao frete. Ela pode mostrar a situação exata, recursos vigentes e os eventos necessários para operar, sem assumir a consulta geral de históricos. Evitar expor CPF, CNH, dados cifrados e contatos desnecessários.

### 6. Interface baseada nas três referências

Reutilizar os componentes, ícones e `ProviderLayout` compatíveis, sem copiar os dados demonstrativos FR-3086, Rafael Souza, Litoral Express, placa ou datas das imagens. Uma feature própria concentra cliente HTTP, tipos, estado do formulário, formulário de registro/interrupção e formulários de retomada e encerramento. Os rótulos e a disponibilidade dependem do frete e da sessão reais.

| Referência | Conteúdo e comportamento planejados |
| --- | --- |
| `1000407677.jpg` | “Registre uma ocorrência”, tipo de registro, data/horário, descrição ou justificativa obrigatória conforme a ação, local opcional, recursos atuais e painel com efeito da confirmação. Para interrupção, o botão “Registrar e interromper execução” confirma a passagem para `INTERROMPIDO`. A ocorrência mantém `EM_EXECUCAO`. “Voltar sem registrar” abandona o formulário sem POST. |
| `1000407679.jpg` | “Retome a execução com segurança”, data/horário, justificativa, recursos confirmados, checkbox de possibilidade de prosseguir e interrupção em aberto com motivo e autor. “Confirmar retomada” muda o estado para `EM_EXECUCAO`. “Voltar ao acompanhamento” mantém a interrupção. |
| `1000407681.jpg` | “Encerre sem marcar como concluído”, data/horário, justificativa, situação opcional da encomenda, checkbox de não conclusão e painel com responsável pelo registro. O botão “Encerrar sem conclusão” confirma o encerramento. “Voltar e manter interrompido” conserva o estado. |

As três telas marcam data e horário como obrigatórios. A primeira mostra o tipo de registro e a justificativa para interrupção. A descrição da ocorrência sem interrupção vem da US-012. A retomada exige justificativa e confirmação de que é possível prosseguir. O encerramento exige justificativa e confirmação de que o frete não foi concluído. Os campos auxiliares sem marca de obrigatoriedade e sua exposição seguem P3. Para a referência verificável dos frames e acesso futuro às capturas, ver P6.

Selecionar retomada ou encerramento no controle de tipos direciona ao formulário correspondente apenas se `INTERROMPIDO`. Não simular que essas ações estão disponíveis durante execução ativa. O início é uma ação no contexto autorizado do frete pronto, sem inventar uma quarta tela completa sem referência.

Na interrupção, na retomada e no encerramento, os botões do acompanhamento usam a elegibilidade da API. O formulário conserva a mesma chave e os mesmos dados enquanto a resposta estiver incerta e o formulário permanecer aberto. A tentativa seguinte recupera o resultado no servidor. Em conflito, mostra a opção de atualizar o acompanhamento antes de nova confirmação. Após receber o recibo, volta à consulta do estado vigente. Sair antes de confirmar apenas consulta o acompanhamento.

Carregamento, falha, falta de permissão, conflito e envio em andamento são estados explícitos. Botões de retorno não fazem submit; campos e seleção não persistem automaticamente. Após confirmação, o retorno não desfaz uma ação já registrada. Em conflito, manter os textos para revisão, recarregar o contexto e exigir nova confirmação; não reaplicar silenciosamente a ação. Após sucesso, apresentar o resultado persistido. Falha posterior de atualização da tela não pode ser descrita como falha de uma gravação já confirmada.

Empilhar os dois cartões em viewport estreito, preservar rótulos, foco, navegação por teclado e leitura dos avisos. O layout de gestão não concede ao motorista acesso às funções administrativas da empresa. A integração de rotas e pontos de entrada deve ser mínima e coordenada com os responsáveis pelos arquivos compartilhados.

### 7. Notificações internas e fronteiras funcionais

Os frames preveem avisos na interrupção (cliente e prestador responsável), retomada (partes envolvidas) e encerramento sem conclusão (cliente e prestador). O RF010 identifica e registra esses fatos de execução. A política efetiva e a entrega dos avisos seguem P4. A operação e seu reenvio produzem um único fato, que serve de referência para a integração aprovada.

Conclusão e registro do recebedor pertencem ao RF012. Substituição durante interrupção pertence ao RF009 e não retoma por si só. Cancelamento depois do início permanece impedido pelo RF011. O término sem conclusão não modifica proposta aceita, valor, condições ou informações de entrega.

### 8. Verificação planejada

JUnit/Mockito: matriz de autores e estados, textos/datas inválidos e confirmações. MockMvc: sessão, CSRF, autor falsificado, motorista colega, motorista substituído e contratos mínimos. PostgreSQL: sequência das ações, eventos de execução, IDs, unicidade de reenvio, rejeição de formulário de ciclo anterior, rollback e concorrência com RF009/RF011. Selenium: os três formulários com Vite/API/banco de teste, retorno sem confirmação, persistência após recarga e layout responsivo. Os testes de cada grupo acompanham a implementação do próprio grupo em `tasks.md`.

Validar os artefatos com `openspec validate rf010-acompanhamento-execucao-ocorrencias --strict`, conferir `openspec status --change rf010-acompanhamento-execucao-ocorrencias` e revisar o diff. Validação estrutural não comprova a implementação da funcionalidade; tarefas de código continuam pendentes.

## Risks / Trade-offs

- **[Conta do autônomo sem motorista próprio]** RF009 mantém essa integração pendente -> consumir a representação definitiva RF002/RF009, sem cadastrar identidades provisórias nesta feature; não declarar o fluxo do autônomo completo sem verificá-lo.
- **[Permissão ampla de outros módulos]** vínculo empresarial poderia autorizar todos os motoristas -> resolver sempre o motorista atual do frete e testar o colega da mesma empresa como terceiro.
- **[Formulário antigo e nova interrupção]** o mesmo estado reaparece em ciclos diferentes -> exigir ID da interrupção e revisão de designação no contexto confirmado.
- **[Datas editáveis]** validar preenchimento e formato. O contrato de armazenamento e exibição segue P2.
- **[Migrações e arquivos compartilhados]** mudanças paralelas podem ocupar versões ou rotas -> conferir PRs/versões e implementar adições próprias antes de integrar pontos compartilhados autorizados.
- **[Referências fora do repositório]** imagens podem não acompanhar uma futura sessão -> registrar seus nomes e comportamentos aqui; disponibilizar as referências originais à equipe antes da conferência visual.
- **[Autorização de implementação]** `AGENTS.md` limita backend atualmente autorizado -> esta proposta planeja a solução, mas somente uma etapa posterior expressamente autorizada pode executar as tarefas de código.

## Migration Plan

1. Após a autorização de implementação, reler a mudança, conferir Git/PRs, contratos atuais e as referências visuais.
2. Aplicar a V13 em ambiente de teste, verificar banco limpo e atualização de V12 com recibos existentes e revisar a numeração antes da integração. P5 se aplica a futuras mudanças de contratos compartilhados.
3. Entregar consulta operacional, regras e comandos transacionais com testes. Validar a integração de avisos com o RF013 após P4.
4. Integrar os três formulários e a ação de início às rotas autorizadas; verificar dados reais, acessibilidade, retorno sem escrita e conflitos.
5. Executar as regressões RF008/RF009/RF011/RF014 e as duas modalidades em banco dedicado. Manter a dependência do autônomo visível até a associação definitiva e os testes correspondentes passarem.
6. Em reversão da implementação, retirar pontos de entrada preservando os eventos de execução já gravados. Qualquer ajuste de esquema usa migração compensatória; não apagar o histórico.

## Open Questions

Este é o registro das decisões pendentes. O andamento independente indicado abaixo pressupõe aprovação da proposta e autorização para implementar.

| ID | Decisão e ponto de alinhamento | Responsável pelo alinhamento | Cenário afetado | Trabalho independente |
| --- | --- | --- | --- | --- |
| P1 | Escolher o papel exibido quando a mesma conta é prestadora e motorista designada. A identidade sempre vem da sessão. Alinhar com P5 se a decisão afetar a exibição geral ou o esquema. | Gustavo e Walison, com Josué se houver impacto no RF014 | Autoria nas quatro ações por conta com ambos os vínculos, em especial no fluxo do autônomo. | Sim. Autorização e registro da conta autora funcionam para todos os perfis, e os casos com contas distintas podem ser verificados separadamente. |
| P2 | Decisão local do RF010. A data e hora digitadas representam o horário civil informado, sem fuso implícito. O acompanhamento operacional mostra esse valor sem conversão. O instante de registro do RF010 permanece em UTC. O RF014 conserva a leitura do registro compartilhado e a semântica dos históricos antigos. | Gustavo e Walison | Persistência e apresentação do horário no acompanhamento autorizado. | Sim. |
| P3 | Decisão local do RF010. Local opcional na ocorrência e na interrupção. Situação opcional da encomenda apenas no encerramento dessa modalidade. Ambos ficam no recibo e são retornados no acompanhamento autorizado ao prestador responsável e ao motorista designado. | Gustavo e Walison | Registro e consulta operacional dos campos auxiliares. | Sim. A projeção geral do RF014 continua consumindo os campos existentes do evento. |
| P4 | Alinhar o contrato para publicar ou consumir os fatos confirmados de interrupção, retomada e encerramento. Rafael define política de destinatários, aviso à conta autora, relevância das ocorrências, eventual aviso de início e tratamento de falhas e duplicatas. `notificacao` referencia frete/solicitação, sem referência direta ao evento. Se for necessário alterar tabela ou contrato, encaminhar ao RF013. Central e envio pertencem ao RF013. | Rafael (RF013), com Gustavo e Walison no contrato de origem | Avisos dos três frames, comportamento no reenvio e eventual aviso de início ou ocorrência sem interrupção. | Sim. Gravar um único fato de execução e validar transições, recusas e retornos pode avançar. O aceite dos avisos aguarda o contrato. |
| P5 | Alinhar o contrato compartilhado de `historico_execucao_frete` para estados anterior/resultante, referência à interrupção e identidade da confirmação. P1 a P3 podem acrescentar metadados. Josué mantém consulta geral, filtros e apresentação. Validar se há registros legados sem evento de início/retomada ou interrupção para derivar o contexto antes de definir seu tratamento. | Gustavo e Walison para escrita do RF010, com Josué (RF014) para compatibilidade e mudanças compartilhadas | Auditoria de transições, recuperação do resultado de reenvio, formulários antigos e leitura dos eventos pelo histórico existente. | Em parte. A lógica de domínio e os formulários podem avançar. Os recibos de interrupção, retomada e encerramento usam tabelas próprias e o RF014 conserva a leitura atual. Mudanças compartilhadas e tratamento adicional de legados ainda exigem alinhamento. |
| P6 | Resolvida. A dupla indicou três JPEGs como referências aprovadas de desktop. Os originais estão em [evidencias/referencias/](evidencias/referencias/). | Gustavo e Walison | Registro/interrupção, retomada e encerramento. | Comparação por título e diferenças por tela no [relatório](implementation-report.md#comparação-com-os-três-jpegs-aprovados). A largura de 390 px verifica responsividade. |
| P7 | Acompanhar RF002/RF009 para associar a conta do autônomo ao motorista próprio e liberar a designação. A implementação atual de RF009 rejeita a designação do autônomo antes de `PRONTO_INICIO`. Não criar vínculo artificial nem liberar o início pelo papel de prestador. | Responsáveis de RF002/RF009, com Gustavo e Walison no aceite do RF010 | Autônomo contratado em `AGUARDANDO_DESIGNACAO` que tenta designar recursos e iniciar. Sem designação, não alcança as quatro ações pelo fluxo normal. | Sim. Validar o fluxo com fretes cuja designação já é suportada. Manter o aceite do autônomo pendente até concluir a dependência. |
