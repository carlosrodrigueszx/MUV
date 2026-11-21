# Design técnico do RF011

## Context

RF011, RN014 e RU005 e protótipo “Frete — Cancelar antes da execução”.

## Decisions

- API: `GET /api/fretes/{id}/cancelamento` consulta os dados necessários à
  confirmação; `POST /api/fretes/{id}/cancelamento` recebe apenas
  `{ "justificativa": "..." }` e retorna 204. O autor vem exclusivamente da
  sessão do RF001; ambos os endpoints exigem vínculo. POST exige CSRF.
- Erros: 400 para justificativa sem conteúdo (incluindo espaços Unicode),
  401 sem sessão no GET ou no POST com token CSRF válido, 403 sem vínculo,
  404 para frete inexistente e 409 para estado incompatível ou cancelamento
  repetido. Um POST sem token CSRF ou com token inválido pode retornar 403
  antes da verificação da sessão. Espaços nas extremidades da justificativa
  são removidos.
- Persistência: reutilizar `JdbcClient`, `Frete`, `Historico_Execucao_Frete`
  (`CANCELAMENTO`) e `Notificacao`. Usar os defaults de ID da V7 para notificações e da V9 para histórico. A V6 pertence ao RF004. Testar em schema novo, preservando o Flyway do schema anterior. Notificar apenas o destinatário oposto ao autor; a justificativa fica
  no histórico, sem truncá-la ao limite da mensagem da notificação.
- A transação do serviço bloqueia a linha de `Frete` com `FOR UPDATE OF f`,
  verifica vínculo/estado e grava estado, evento e notificação juntos.
  UPDATE também condiciona o estado a AGUARDANDO_DESIGNACAO/PRONTO_INICIO.
  Falha em qualquer gravação desfaz as três; sequências podem deixar lacunas.
- Contrato RF010: início e demais transições devem bloquear a mesma linha,
  reler o estado e fazer UPDATE condicional na mesma transação do evento.
  Nunca retornar de um estado iniciado para um estado pré-execução, nem
  sobrescrever CANCELADO com dados lidos antes do bloqueio. A branch ainda
  não implementa RF010: testar disputa com o UPDATE condicional equivalente
  no PostgreSQL, sem criar endpoint de início neste requisito.
- Depois de obter o bloqueio do frete, a transação captura um único horário
  UTC com `timezone('UTC', clock_timestamp())` no PostgreSQL e grava o mesmo
  valor no histórico e na notificação. O horário reflete o cancelamento
  após a espera pelo bloqueio. Como o schema usa TIMESTAMP sem fuso, a API
  declara UTC para o registro e a interface o converte para o fuso do navegador.
  O agendamento existente é exibido como horário local sem reinterpretá-lo.
- A rota `/fretes/:freteId/cancelar` usa dados reais, sem mocks em produção.
  O botão de confirmação exige conteúdo e confirmação explícita; envios em
  andamento ficam bloqueados. Após sucesso, consulta novamente o registro
  persistido e mostra autor/data/hora/motivo; erro de atualização não deve
  ser apresentado como falha do cancelamento já confirmado.
- Referência visual: captura “Frete — Cancelar antes da execução” fornecida
  pelo usuário. O cabeçalho do RF011 reaproveita os ícones do projeto e
  compartilha o alinhamento do conteúdo. “Meus fretes” aponta para o histórico
  do perfil que consulta o frete, e o avatar abre “Minha conta”. Os controles
  de troca de perfil e notificações informam que essas ações ainda estão
  indisponíveis. A justificativa começa com uma linha e cresce com o conteúdo.
- No RF008, o roteador mantém as rotas da main e a rota do RF011. A página de frete contratado usa o identificador retornado pela API e exibe a ação somente nos estados elegíveis.
- No RF014, o prefixo `FR-` identifica frete e `SL-` identifica solicitação. O rótulo `Contratado` agrega os dois estados elegíveis, `EM_EXECUCAO` e `INTERROMPIDO`. A ação direta fica no RF008, que fornece estado exato. O RF014 já mapeia o estado `CANCELADO` e o evento `CANCELAMENTO`. O GET de cancelamento devolve também `frete.contratadoEm`; o botão de retorno seleciona o ID e o mês da contratação no histórico do perfil autenticado, preservando os filtros e a autorização do RF014. Quando o período é omitido na API, o SQL tipa explicitamente o argumento nulo como timestamp para manter a consulta sem recorte temporal.
- No RF009, os comandos de designação e substituição adquirem o mesmo bloqueio e revalidam o estado antes da escrita. O aceite de novo acordo também bloqueia a linha do frete e exige estado compatível.
- Dependência RF013: não há serviço de notificações nesta branch; a entrega
  é a inserção transacional na tabela existente. Central, envio externo e
  leitura de notificações permanecem no RF013.
- Não modificar valores, condições, propostas ou dados financeiros.
