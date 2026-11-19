# RF011 — Cancelamento de Fretes

## Objetivo

Permitir que o cliente ou o prestador responsável cancele um frete
contratado antes do início da execução, mantendo o histórico e avisando
a outra parte.

## Referências

RF011, RN014 e RU005 do documento de requisitos do MUV.
Protótipo “Frete — Cancelar antes da execução”.

## Critérios de aceite

1. Dado um frete contratado que ainda não começou, quando o cliente
   ou o prestador responsável confirma o cancelamento com uma
   justificativa válida, o sistema muda a situação para Cancelado,
   registra autor, data, hora e justificativa e notifica a outra parte.
2. Se a justificativa estiver vazia ou contiver apenas espaços, o
   sistema rejeita a operação e mantém o frete como estava.
3. Se a execução já começou, inclusive se estiver interrompida,
   o sistema rejeita o cancelamento. A API aplica a mesma regra
   mesmo quando a tela já estava aberta antes do início.
4. Uma conta sem vínculo com o frete não pode cancelá-lo.
5. Um segundo envio para um frete já cancelado não duplica o registro
   nem a notificação.
6. O cancelamento não aplica multa, cobrança ou reembolso automático.

## Dependências e validação

- Usar os estados do frete contratado definidos pelo projeto.
- Coordenar com o RF010 a disputa entre início e cancelamento:
  somente uma transição pode prevalecer.
- Usar o mecanismo de notificações do RF013. A central de notificações
  não faz parte desta entrega.
- Validar com dois perfis autorizados, uma conta sem vínculo e fretes
  de teste antes e depois do início da execução.

## Tarefas

- [x] Conferir o schema e os contratos existentes de frete, histórico
      e notificação na branch-base.
- [x] Escrever testes para os critérios de aceite.
- [x] Implementar a validação e o registro do cancelamento na API.
- [x] Integrar a tela de justificativa e confirmação.
- [x] Executar os testes e conferir o registro persistido no navegador.

## Decisões técnicas (implementação RF011)

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
  (`CANCELAMENTO`) e `Notificacao`. Preservar a V6 existente para geração de
  IDs. Notificar apenas o destinatário oposto ao autor; a justificativa fica
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
- Dependência RF014: nesta branch o histórico é um serviço local vazio.
  O PR aberto #15 altera histórico, AccountNavbar e ClientePrestadores;
  esses arquivos serão preservados. O fluxo real deve ligar cada frete
  elegível à rota acima após integração do RF014. Validar por URL com dados
  controlados enquanto não existir esse ponto de entrada.
- Dependência RF013: não há serviço de notificações nesta branch; a entrega
  é a inserção transacional na tabela existente. Central, envio externo e
  leitura de notificações permanecem no RF013.
- Não modificar valores, condições, propostas ou dados financeiros.

## Matriz de validação

| Critério | Cobertura |
| --- | --- |
| 1 | Cliente/prestador nos dois estados; API com sessão; histórico e notificação com o mesmo horário UTC após o bloqueio no PostgreSQL; tela com confirmação |
| 2 | Serviço e API: null, vazio, espaços, tabulação e espaços Unicode; nenhuma gravação |
| 3 | Todos os estados não elegíveis; tela aberta antes do início; disputa SQL compatível com RF010 |
| 4 | Serviço e API: terceiro, acesso sem sessão e tentativa de falsificar autor; MockMvc verifica POST anônimo com CSRF válido (401), ausente ou inválido (403) |
| 5 | Segundo envio e dois cancelamentos concorrentes; somente um evento/notificação |
| 6 | Valor e condições permanecem iguais após sucesso; nenhuma operação financeira |
| Atomicidade/V6 | Flyway em schema PostgreSQL isolado; IDs automáticos com registros prévios; falha forçada na notificação reverte estado e evento |

## Resultado da validação

- 39 testes do RF011 aprovados na revisão. São 17 de serviço, incluindo
  o teste original, e 22 com PostgreSQL 18, incluindo MockMvc, V6 sobre dados
  existentes, rollback após falha na notificação e disputa nos dois sentidos
  com o SQL de início previsto para o RF010. A revisão verificou o horário
  UTC capturado após a espera pelo bloqueio e o POST anônimo com token CSRF
  ausente, inválido ou válido.
- Com `-Dmuv.rf011.e2e=true`, os 3 testes Selenium passaram no Chromium.
  Eles cobrem confirmação e persistência após recarregar, rejeição após o
  início e bloqueio de terceiros e sessões ausentes. Sem a propriedade,
  o JUnit pulou os 3 testes antes de inicializar o Spring.
- Na validação inicial, 73 testes de backend foram aprovados, incluindo os
  37 testes então existentes do RF011.
  O perfil de autenticação existente exigiu `-Dselenium.browser=chrome`
  para inicializar o contexto dos seus 9 testes.
- Build do frontend, lint dos arquivos frontend alterados e `git diff --check`.
- Na revisão visual, os três testes Selenium passaram novamente. A validação
  conferiu os links de histórico e conta, o aviso de notificações, o foco e
  a expansão da justificativa, e os cartões empilhados sem rolagem horizontal.

## Como reproduzir a validação local

Com Java 21 e um PostgreSQL dedicado a testes, defina `MUV_RF011_DB_URL`
(por exemplo, `jdbc:postgresql://127.0.0.1:55411/postgres`) e, se necessário,
`MUV_RF011_DB_USER` e `MUV_RF011_DB_PASSWORD`. A conta precisa poder criar
schemas de teste. As fixtures são carregadas em schemas isolados e usam
somente dados sintéticos.

No diretório `backend`:

```sh
./mvnw -Dtest=FreightCancellationServiceTest,FreightCancellationRulesTest,FreightCancellationIntegrationTest,FreightCancellationMigrationTest test
```

No diretório `frontend`:

```sh
npm run build
npx eslint src/features/freight-cancellation src/app/Router.tsx
npm run dev -- --host 127.0.0.1 --strictPort
```

O JUnit só executa `FreightCancellationNavigationTest` quando a propriedade
`-Dmuv.rf011.e2e=true` é informada. Para rodar os três testes, deixe o Vite
na porta 5173 e a porta 8080 livre. Execute o comando no diretório `backend`.
O teste inicia o Spring Boot com o PostgreSQL de teste e usa o WebDriver
do projeto. Caso necessário, informe o driver instalado com
`-Dwebdriver.chrome.driver=/caminho/para/chromedriver` ou selecione o Firefox
com `-Dselenium.browser=firefox -Dwebdriver.gecko.driver=/caminho/para/geckodriver`.

```sh
./mvnw -Dtest=FreightCancellationNavigationTest -Dmuv.rf011.e2e=true -Dselenium.base-url=http://127.0.0.1:5173 test
```

O Selenium salva as capturas em `backend/target/rf011-browser/`. A captura
`rf011-desktop.png` usa a mesma largura útil de 1.351 px da comparação
com o protótipo. `rf011-mobile.png` usa 390 px e mostra os cartões empilhados.
Se as portas padrão estiverem ocupadas, ajuste a URL em `selenium.base-url`,
a porta Spring em `server.port` e o destino do proxy `/api` do Vite.

Para confirmar que a classe é pulada por padrão, rode o comando abaixo.
Essa execução dispensa Vite, PostgreSQL e WebDriver.

```sh
./mvnw -Dtest=FreightCancellationNavigationTest test
```
