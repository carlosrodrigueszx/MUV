# Executar o MUV localmente

Os comandos abaixo partem da raiz do repositório e usam Bash. Você pode iniciar com um banco vazio ou usar a simulação de pessoas e encomendas em um banco separado.

## Pré-requisitos

- Java 21 e Maven. O wrapper `backend/mvnw` também está disponível.
- Node.js 22.12 ou superior e npm.
- Docker com Compose v2 e a porta 5432 livre para o PostgreSQL 16.
- OpenSSL para gerar a chave local usada no cadastro. Guarde a mesma chave enquanto reutilizar o banco e nunca a versione.

## 1. Execução com banco vazio

Suba o PostgreSQL local e crie um banco de desenvolvimento. Rode o comando `createdb` apenas na primeira preparação desse banco.

```bash
docker compose -f docker-compose.test.yaml up -d --wait
docker compose -f docker-compose.test.yaml exec -T postgres-teste \
  createdb -U dev muv_dev
npm --prefix frontend ci
```

No terminal do backend, gere a chave uma vez e reutilize seu valor nas próximas sessões:

```bash
export MUV_AUTH_PASSENGER_KEY="$(openssl rand -base64 64 | tr -d '\n')"
export DB_URL=jdbc:postgresql://localhost:5432/muv_dev
export DB_USERNAME=dev
export DB_PASSWORD=dev
export SPRING_PROFILES_ACTIVE=dev
mvn -f backend/pom.xml spring-boot:run
```

O Flyway aplica as migrações na inicialização. Aguarde o Spring Boot subir na porta 8080. Para usar outro PostgreSQL, ajuste as três variáveis `DB_*` e mantenha a porta HTTP do backend em 8080 se usar o proxy do Vite sem alterações.

No terminal do frontend:

```bash
npm --prefix frontend run dev
```

Abra <http://localhost:5173>. O Vite encaminha `/api` para o backend. Com o banco vazio de contas, use **Criar conta** para começar. A recuperação de senha por e-mail exige SMTP e remetente configurados. Sem perfis de custo configurados, a interface mostra a estimativa como indisponível.

## 2. Simulação com dados fictícios

Use apenas o PostgreSQL local de teste. As credenciais conhecidas e os valores desta seção são exclusivos da simulação. Pare o backend de desenvolvimento para liberar a porta 8080. Se começar pela simulação, execute `docker compose -f docker-compose.test.yaml up -d --wait` e `npm --prefix frontend ci`. O Compose já cria o banco `app_teste`.

Crie a configuração temporária de preços. Ela ativa teto de 50% acima da estimativa nas duas modalidades e piso da oferta inicial de pessoas.

```bash
data_muv=$(TZ=America/Fortaleza date +%F)
cat > /tmp/muv-simulacao.properties <<PROPERTIES
muv.estimativa.pessoa.consumo-km-litro=10
muv.estimativa.pessoa.preco-litro=6.10
muv.estimativa.pessoa.fonte-combustivel=Fixture MUV
muv.estimativa.pessoa.data-combustivel=$data_muv
muv.estimativa.pessoa.validade-dias=1
muv.estimativa.pessoa.operacao-por-km=0.40
muv.estimativa.pessoa.remuneracao=15.00
muv.estimativa.pessoa.alerta-percentual=25
muv.estimativa.pessoa.piso-fator=1
muv.estimativa.pessoa.piso-minimo=0
muv.estimativa.pessoa.teto-ativo=true
muv.estimativa.pessoa.teto-percentual=50

muv.estimativa.encomenda.consumo-km-litro=8
muv.estimativa.encomenda.preco-litro=6.00
muv.estimativa.encomenda.fonte-combustivel=Fixture MUV
muv.estimativa.encomenda.data-combustivel=$data_muv
muv.estimativa.encomenda.validade-dias=1
muv.estimativa.encomenda.operacao-por-km=0.50
muv.estimativa.encomenda.remuneracao=20.00
muv.estimativa.encomenda.alerta-percentual=25
muv.estimativa.encomenda.teto-ativo=true
muv.estimativa.encomenda.teto-percentual=50
PROPERTIES

DB_URL=jdbc:postgresql://localhost:5432/app_teste DB_USERNAME=dev DB_PASSWORD=dev \
  SPRING_PROFILES_ACTIVE=test \
  SPRING_CONFIG_ADDITIONAL_LOCATION=file:/tmp/muv-simulacao.properties \
  mvn -f backend/pom.xml spring-boot:run
```

Depois que o Flyway concluir, execute o bloco abaixo em outro terminal, uma vez em `app_teste` limpo. São reutilizadas as fixtures de [propostas RF007](backend/src/test/resources/db/rf007-solicitacoes.sql). As distâncias gravadas tornam o cálculo independente do resultado do roteador, embora o mapa ainda possa ficar indisponível.

```bash
docker compose -f docker-compose.test.yaml exec -T postgres-teste \
  psql -U dev -d app_teste -v ON_ERROR_STOP=1 \
  < backend/src/test/resources/db/rf007-solicitacoes.sql

docker compose -f docker-compose.test.yaml exec -T postgres-teste \
  psql -U dev -d app_teste -v ON_ERROR_STOP=1 <<'SQL'
BEGIN;
UPDATE usuario
SET senha = '$2b$12$abcdefghijklmnopqrstuug/YZcuGyJ6iuO0c.sQODBMvokc9TFSm'
WHERE id IN (907001, 907002);
UPDATE solicitacao_frete
SET percurso_km = 120.00, data_hora_pretendida = CURRENT_TIMESTAMP + INTERVAL '2 days'
WHERE id = 907031;
UPDATE solicitacao_frete
SET percurso_km = 80.00, data_hora_pretendida = CURRENT_TIMESTAMP + INTERVAL '2 days'
WHERE id = 907032;
COMMIT;
SQL
```

Inicie ou mantenha o frontend na porta 5173 com `npm --prefix frontend run dev`. Entre em <http://localhost:5173/entrar> com uma das contas abaixo. A senha das duas é `Teste-MUV-2026!`.

| Perfil | E-mail |
| --- | --- |
| Prestador autorizado nas duas modalidades | `autor907@example.test` |
| Cliente titular das solicitações | `cliente907@example.test` |

Como prestador, abra a avaliação de cada solicitação:

| Modalidade | Avaliação | Distância | Estimativa | Alerta acima de | Teto |
| --- | --- | ---: | ---: | ---: | ---: |
| Pessoas | [SL-907031](http://localhost:5173/prestador/solicitacoes/907031/avaliacao) | 120 km | R$ 136,20 | R$ 170,25 | R$ 204,30 |
| Encomendas | [SL-907032](http://localhost:5173/prestador/solicitacoes/907032/avaliacao) | 80 km | R$ 120,00 | R$ 150,00 | R$ 180,00 |

1. Expanda a estimativa e confira a composição. Os valores iniciais das fixtures excedem o teto, portanto **Aceitar** deve ficar bloqueado.
2. Abra **Fazer contraproposta**. Digite um centavo acima do teto e confira o bloqueio. Ajuste para o teto, informe uma justificativa e envie.
3. Use outra sessão de navegador para entrar como cliente. Abra a [comparação de pessoas](http://localhost:5173/cliente/solicitacoes/907031/propostas) ou a [comparação de encomendas](http://localhost:5173/cliente/solicitacoes/907032/propostas). Confira o valor recebido e finalize o aceite.
4. Para simular referência vencida, mude a `data-combustivel` da modalidade para `2000-01-01` e reinicie o backend. Confira o bloqueio em uma solicitação ainda aberta e sem proposta enviada pelo prestador.

O piso configurado vale para novas ofertas iniciais de pessoas. Para conferir pela interface, use um prestador com veículo compatível e publique uma solicitação direcionada com endereços que o roteador consiga localizar. Consulte a estimativa e tente enviar um centavo abaixo do mínimo exibido. O piso seria R$ 121,20 caso a distância calculada fosse 120 km. A fixture da seção anterior já contém propostas iniciais persistidas. Ela serve para testar a resposta do prestador.

## 3. Validar e encerrar

```bash
npm --prefix frontend run build
node --test frontend/test/*.test.mjs
mvn -f backend/pom.xml \
  -Dtest=EstimativaFreteServiceTest,EnvioSolicitacaoEstimativaTest,PercursoServiceTest,PropostaServiceTest,PropostaControllerTest \
  test
```

Para os testes de integração, use um `app_teste` limpo, antes de carregar as fixtures manuais. As suítes carregam e limpam seus próprios dados.

```bash
DB_URL=jdbc:postgresql://localhost:5432/app_teste DB_USERNAME=dev DB_PASSWORD=dev \
  mvn -f backend/pom.xml \
  -Dtest=PropostaIntegrationTest,ContratacaoIntegrationTest,FluxoDemonstracaoIntegrationTest test
```

Encerre o frontend e o backend com `Ctrl+C`. Para parar o banco local preservando os dados:

```bash
docker compose -f docker-compose.test.yaml stop postgres-teste
```
