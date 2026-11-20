# RF004 — Cadastro e Gestão de Veículos

## Escopo identificado

O ZIP do projeto não contém uma especificação funcional separada para RF004. A implementação segue os fluxos e campos da tela existente de gestão de veículos e o modelo `Veiculo` da migration inicial. A tela oferece cadastro, listagem e edição; não apresenta exclusão.

## Campos

| Tela | Persistência |
| --- | --- |
| Tipo: transporte de passageiros | `modalidade = PESSOA`; capacidade em `capacidade_passageiros` |
| Tipo: transporte de carga | `modalidade = ENCOMENDA`; capacidade em `capacidade_carga_kg` |
| Marca | `marca` |
| Modelo | `modelo` |
| Ano | `ano` |
| Placa | `placa`, normalizada em maiúsculas e sem hífen |
| Cor | `cor` |
| Características adicionais | `descricao` (adicionada pela migration V6) |
| Situação | `ativo`; novos veículos são cadastrados ativos |

## API

| Método | Endpoint | Ação |
| --- | --- | --- |
| GET | `/api/veiculos` | Lista veículos do prestador autenticado |
| GET | `/api/veiculos/{id}` | Consulta veículo do prestador autenticado |
| POST | `/api/veiculos` | Cadastra veículo para o prestador autenticado |
| PUT | `/api/veiculos/{id}` | Atualiza veículo do prestador autenticado |

As operações exigem sessão autenticada e token CSRF nas alterações. A associação ao prestador vem da conta da sessão; o cliente não informa nem escolhe o proprietário. Placa duplicada retorna conflito. Registros de outro prestador não são retornados nem podem ser editados.

## Testes

- Unitários: mapeamento de modalidade e capacidade, normalização de placa, conta sem perfil de prestador e veículo inexistente.
- Integração: criação, leitura, edição, persistência das capacidades e descrição, placa duplicada e isolamento entre prestadores.
- Selenium: fluxo autenticado de cadastro e edição pela interface, seguido de verificação no banco de teste.

## Execução

Banco de testes local (não use banco de desenvolvimento ou produção):

```powershell
docker compose -f docker-compose.test.yaml up -d
cd backend
.\mvnw.cmd -B -ntp verify
```

Build e lint do frontend:

```powershell
cd frontend
npm ci
npm run build
npm run lint
```

O teste Selenium usa as variáveis `MUV_E2E_PROVIDER_EMAIL` e `MUV_E2E_PROVIDER_PASSWORD`, com uma conta de prestador exclusiva no banco de teste, e pressupõe o frontend em `SELENIUM_BASE_URL` (padrão `http://localhost:5173`) e o backend apontando ao mesmo banco. Inicie frontend e backend com o perfil `test` antes do teste. Evidências são salvas em `docs/testes/evidencias/RF004`.
