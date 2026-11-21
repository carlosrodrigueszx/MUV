# Design técnico do RF009

## Context

Veja `proposal.md` para a motivação e `specs/designacao-motoristas-veiculos/spec.md` para o contrato de comportamento.

A migração inicial já possui `frete.motorista_atual_id`, `frete.veiculo_atual_id`, os estados `AGUARDANDO_DESIGNACAO` e `PRONTO_INICIO`, a tabela `historico_execucao_frete` e a solicitação de alteração de acordo. A FK composta de `frete` exige que o motorista pertença a `vinculo_motorista` e que o veículo pertença ao prestador responsável. Não há entidade JPA de frete; propostas, veículos e histórico usam consultas JDBC em suas partes transacionais ou de leitura.

O RF003 mantém motorista, vínculo e verificação. O RF004 mantém veículo, modalidade, atividade, passageiros e carga em quilogramas. O cadastro atual do autônomo guarda a habilitação cifrada, mas não cria registro em `motorista` nem vínculo próprio. O RF002, sob responsabilidade do mesmo integrante, será implementado apenas na próxima Sprint e definirá essa representação. O RF008 ainda não cria fretes reais. Os frames locais cobrem designação e substituição no portal do prestador; não existe frame equivalente para a consulta do cliente.

## Goals / Non-Goals

**Goals:**

- Manter autorização, compatibilidade, atualização do frete, histórico e notificação na mesma fronteira transacional.
- Reutilizar motoristas, vínculos, veículos, sessão e layout existentes, sem listas estáticas.
- Definir contratos que possam ser consumidos pelo RF008 e pelo RF010 sem acoplamento às suas interfaces.
- Preservar o trabalho paralelo mediante migração aditiva e arquivos próprios da feature sempre que possível.
- Permitir que o núcleo organizacional seja implementado e validado antes do RF002, sem criar solução temporária para o autônomo e sem encerrar a mudança prematuramente.

**Non-Goals:**

- Implementar a aceitação de proposta do RF008 ou os comandos de execução do RF010.
- Criar regras de volume, categoria veicular ou agenda sem dados persistidos que as sustentem.
- Introduzir um novo tipo de prestador cooperativa. Uma cooperativa já representada como organização usa o mesmo fluxo; um subtipo próprio exige mudança coordenada no RF002.
- Alterar valor ou condições sem resposta explícita do cliente.
- Projetar uma tela de cliente sem frame ou definição visual aprovada.

## Decisions

### 1. API orientada ao frete e à identidade da sessão

O portal do prestador consulta `GET /api/prestador/fretes/{freteId}/designacao`, envia a primeira designação em `POST /api/prestador/fretes/{freteId}/designacao` e solicita troca em `POST /api/prestador/fretes/{freteId}/substituicoes`. Nenhum comando recebe o prestador autor como dado confiável; ele é resolvido pela sessão e comparado com `prestador_responsavel_id`.

A leitura devolve versão ou marcador de concorrência, resumo do frete, designação atual, motoristas elegíveis, veículos com o resultado de compatibilidade e condições vigentes. O cliente usa leitura própria, como `GET /api/cliente/fretes/{freteId}/designacao`, limitada ao passageiro da solicitação. Preferiu-se contrato próprio a reutilizar o histórico do RF014, pois o RF009 precisa da situação atual e de ações, enquanto o histórico agrega registros para consulta.

### 2. Repositório JDBC e serviço transacional

A implementação mantém o padrão já usado por propostas, veículos e histórico: DTOs de fronteira, serviço com regras e repositório JDBC para consultas e gravações de frete. Não será introduzida uma entidade JPA parcial para `frete`, porque isso criaria duas representações concorrentes da mesma tabela.

O comando bloqueia o frete com `SELECT ... FOR UPDATE`, resolve novamente vínculo, verificação e veículo dentro da transação e só então atualiza o estado, insere o evento e cria a notificação. Uma revisão ou versão retornada pela leitura permite rejeitar confirmação baseada em estado antigo com conflito HTTP 409.

### 3. Compatibilidade limitada a dados verificáveis

Para pessoas, a compatibilidade exige veículo ativo, próprio, modalidade `PESSOA` ou `AMBOS` e capacidade de passageiros suficiente. Para encomendas, exige modalidade `ENCOMENDA` ou `AMBOS` e capacidade em quilogramas suficiente. O motorista organizacional precisa de vínculo `VERIFICADO` com o prestador.

Metros cúbicos, conflito de agenda e categoria de CNH não serão apresentados como validados. A alternativa de inferir volume pelas dimensões ou disponibilidade pela duração foi rejeitada porque o cadastro não informa capacidade volumétrica nem uma agenda completa e isso criaria regra não especificada.

### 4. Entrega em duas etapas e dependência do RF002

A primeira etapa implementa consulta, compatibilidade, designação e substituição para organizações usando `vinculo_motorista`, além dos contratos comuns, histórico, notificações e consulta do cliente. Ela não altera o cadastro do autônomo, não cria motorista fictício e não decifra a habilitação para formar um modelo provisório. Se um autônomo alcançar o comando antes da integração, a API rejeita a operação de forma explícita e sem gravação parcial, informando que sua identidade de motorista ainda não está associada.

Na próxima Sprint, o RF002 definirá a representação definitiva do autônomo como motorista. A segunda etapa do RF009 adaptará apenas a fronteira que resolve o executor: cada `prestador_autonomo` deverá apontar para um `motorista` associado à mesma conta e possuir o vínculo necessário para satisfazer as FKs. Novos cadastros criarão a relação na mesma transação; contas legadas serão tratadas por rotina idempotente aprovada no RF002.

O serviço de designação separará a resolução do executor das demais regras, para que a integração posterior não altere compatibilidade de veículos, estados, histórico ou contratos HTTP. As alternativas de deixar `motorista_atual_id` vazio, duplicar o cadastro dentro do RF009 ou guardar identificador fictício foram rejeitadas por quebrarem integridade e criarem conflito com o RF002. A mudança OpenSpec permanece ativa depois da primeira etapa e só poderá ser concluída e arquivada quando os cenários do autônomo passarem.

### 5. Estado e histórico da designação

A primeira designação é aceita apenas em `AGUARDANDO_DESIGNACAO` e produz `PRONTO_INICIO`. Cada evento de designação ou substituição grava o par completo resultante de motorista e veículo, mesmo quando apenas um deles mudou. O par anterior é obtido do estado bloqueado e pode ser apresentado junto do evento resultante, mantendo uma sequência reconstruível sem texto livre como fonte de verdade.

Substituição direta é aceita em `PRONTO_INICIO` ou `INTERROMPIDO`. Em interrupção, o estado permanece `INTERROMPIDO`; uma retomada pertence ao RF010. Em `EM_EXECUCAO`, o prestador precisa primeiro registrar a interrupção autorizada. Essa fronteira evita troca silenciosa durante a execução ativa.

### 6. Alteração de condições por acordo explícito

Quando a troca modifica valor, condições ou o profissional autônomo originalmente contratado, o comando cria `solicitacao_alteracao_acordo` pendente sem alterar a designação atual. A resposta do cliente aplica ou rejeita a solicitação sob bloqueio do frete. No aceite, os recursos são revalidados, as condições vigentes são atualizadas e o evento `SUBSTITUICAO_COM_ACORDO` é criado.

A mudança usa identificadores gerados e restrições para impedir mais de uma resposta e aplicação duplicada. O desenho evita reutilizar o aceite de proposta do RF008, pois a contratação já existe e o objeto alterado é o frete vigente.

### 7. Interface sem dados fictícios

A feature React terá componentes próprios para contexto, seleção, validações, designação atual e substituição, reutilizando `ProviderLayout`, Tailwind e o cliente HTTP já adotado. Os PNGs e JSONs locais são a referência autorizada para as duas telas do prestador. Estados de carregamento, vazio, erro, conflito e confirmação vêm da API; exemplos do frame não entram como dados fixos.

O detalhe do cliente consumirá a leitura de designação, mas sua edição visual fica fora desta mudança até existir frame ou definição aprovada. Nesta mudança, a consulta do cliente será entregue como API autenticada e coberta por testes de autorização, sem inventar uma nova página.

### 8. Notificação e privacidade

Designações iniciais não exigem notificação pela regra atual; substituições e solicitações de novo acordo criam notificação para o cliente na mesma transação. A consulta do cliente expõe somente nome do motorista e identificação/características pertinentes do veículo. CPF, CNH, telefone, dados cifrados e verificações internas não fazem parte da resposta.

### 9. Estratégia de testes

JUnit e Mockito isolam decisões de estado, elegibilidade, compatibilidade, substituição e acordo. MockMvc cobre sessão, autorização, validação, conflitos e contratos. Testes de integração com PostgreSQL verificam FKs, bloqueio, atomicidade, histórico, notificações e concorrência. Selenium cobre as telas do prestador usando Vite e API reais com fixtures exclusivas; o fluxo completo a partir da contratação será ativado quando o RF008 estiver disponível. A primeira etapa verifica também que autônomos ainda não associados falham sem escrita; a segunda substitui esse cenário pelos testes positivos de cadastro e designação integrados ao RF002.

## Risks / Trade-offs

- **[RF008 ausente]** O fluxo real não produz fretes contratados → usar fixtures controladas e manter contrato explícito de entrada `AGUARDANDO_DESIGNACAO`; repetir a validação após integrar o RF008.
- **[RF002 será entregue depois]** O RF009 não poderá atender autônomos na primeira etapa → isolar a resolução do executor, rejeitar o caso sem escrita parcial, manter as tarefas pendentes e não arquivar a mudança até concluir a integração.
- **[Migrações paralelas]** Outra branch pode ocupar a próxima versão Flyway → escolher a versão somente após sincronizar a base e conferir `flyway_schema_history`; nunca renumerar migração já aplicada sem plano da equipe.
- **[Protótipo promete dados inexistentes]** Volume e agenda poderiam induzir falsa compatibilidade → ocultar ou marcar como não avaliado até uma especificação futura ampliar o cadastro.
- **[Consulta do cliente sem design]** Uma tela inventada violaria a fonte visual do projeto → implementar contrato e testes, solicitando o frame antes da interface correspondente.
- **[Cooperativa sem subtipo]** Não há representação própria no domínio → aplicar o fluxo organizacional somente às organizações existentes e coordenar eventual ampliação com RF002.

## Migration Plan

1. Antes de editar, atualizar referências remotas e conferir branch, status, worktrees, PRs e versões Flyway em uso.
2. Na primeira etapa, introduzir somente a migração aditiva necessária à revisão da designação e aos identificadores/estados do acordo, preservando cadastro e tabelas do autônomo.
3. Publicar o contrato de leitura e as regras de domínio com fixtures de frete; depois comandos transacionais e interface do prestador organizacional.
4. Na próxima Sprint, após o RF002 definir o modelo, criar a migração específica da relação autônomo–motorista e integrar cadastros novos e legados sem renumerar migrações já aplicadas.
5. Integrar com RF008 e RF010 em banco limpo e em cópia controlada de banco existente, verificando a sequência das migrações e os dois tipos de prestador.
6. Somente após os testes do autônomo, marcar as tarefas finais, sincronizar a especificação e arquivar a mudança.
7. Em reversão, remover rotas e pontos de entrada, mantendo registros de frete, histórico e notificações já produzidos. Alterações de esquema só serão revertidas por migração compensatória, sem apagar dados.

## Open Questions

- As cooperativas serão cadastradas como empresas na Sprint 2 ou ganharão um subtipo próprio em mudança futura?
