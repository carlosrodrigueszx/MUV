# Validação do RF011

## Matriz de validação

| Critério | Cobertura |
| --- | --- |
| 1 | Cliente/prestador nos dois estados; API com sessão; histórico e notificação com o mesmo horário UTC após o bloqueio no PostgreSQL; tela com confirmação |
| 2 | Serviço e API: null, vazio, espaços, tabulação e espaços Unicode; nenhuma gravação |
| 3 | Todos os estados não elegíveis; tela aberta antes do início; disputa SQL compatível com RF010 |
| 4 | Serviço e API: terceiro, acesso sem sessão e tentativa de falsificar autor; MockMvc verifica POST anônimo com CSRF válido (401), ausente ou inválido (403) |
| 5 | Segundo envio e dois cancelamentos concorrentes; somente um evento/notificação |
| 6 | Valor e condições permanecem iguais após sucesso; nenhuma operação financeira |
| RF008 → RF011 → RF014 | Frete contratado usa ID distinto da solicitação; ação restrita aos estados elegíveis; histórico mostra situação, evento e motivo sem filtro de período na API e no mês efetivo da contratação no navegador |
| RF009 concorrente | Designação e substituição aguardam o bloqueio do frete e rejeitam a transição depois do cancelamento |
| Atomicidade/IDs | Flyway integrado em schema PostgreSQL isolado; IDs automáticos com registros prévios; falha forçada na notificação reverte estado e evento |

## Resultado na base integrada (d113b85)

- `mvn clean test` com a seleção de regressão descrita abaixo aprovou 112 testes, sem falhas ou saltos. São 44 do RF011, incluindo 3 Selenium e 23 de integração da API, e 68 de regressão do RF008, RF009 e RF014. O navegador confirmou o retorno ao frete cancelado no mês de contratação, com estado, evento e justificativa.
- PostgreSQL 18.7 no banco dedicado `muv_rf011_integration`. O Flyway aplicou 10 migrations até a V9 em schema novo e validou a atualização de V6 para V9 com registros preexistentes. A V7 e a V9 geraram IDs acima dos registros anteriores.
- As operações reais de designação e substituição aguardaram a linha bloqueada e retornaram 409 após o cancelamento. O RF010 ainda não possui comando de início nessa base. Os dois sentidos da corrida foram cobertos pelo UPDATE condicional no PostgreSQL.
- A consulta RF014 sem período passou após tipar o argumento nulo no SQL. A validação estrita do OpenSpec, o build, o lint dos arquivos afetados, os dois testes Node e a checagem do diff passaram.

## Evidência histórica da branch anterior (a4aa0d1)

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

Execute na raiz do projeto.

```sh
openspec validate rf011-cancelamento-fretes --strict
git diff --check
git diff --cached --check
```

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
npx eslint src/features/freight-cancellation src/app/Router.tsx src/features/contratacao/Contratacao.tsx src/pages/FreightHistory.tsx
node --test test/*.test.mjs
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

Para repetir a regressão combinada com a configuração de banco dos colegas, aponte também `MUV_LOCAL_DB_URL` e `DB_URL` para o banco de teste e alinhe `MUV_LOCAL_DB_USER`, `MUV_LOCAL_DB_PASSWORD`, `DB_USERNAME` e `DB_PASSWORD`. Com o Vite e o ChromeDriver disponíveis, execute o comando no diretório `backend`.

```sh
mvn -Dselenium.browser=chrome -Dmuv.rf011.e2e=true \
  -Dselenium.base-url=http://127.0.0.1:5173 \
  '-Dtest=FreightCancellation*Test,*FreightHistory*Test,!FreightHistoryNavigationTest,*Designacao*Test,!DesignacaoSystemTest,Substituicao*Test,AcordoControllerTest,ContratacaoIntegrationTest' \
  clean test
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
