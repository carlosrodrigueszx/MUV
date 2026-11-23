# Design técnico do RF006

## 1. Visão Arquitetural
O desenho contempla exclusivamente a consulta e filtragem de solicitações abertas no painel do prestador (**RF006**), alinhado à arquitetura em camadas do MUV (Interface Web → Controllers → Serviços/Domínio → Repositórios → PostgreSQL) e aos artefatos já existentes na branch `main`.

## 2. Domínio e Regras do RF006
- **`SolicitacaoFrete` (e especializações `SolicitacaoPessoa` / `SolicitacaoEncomenda`):** Considera solicitações em estado aberto e não direcionadas a prestador específico para exibição no painel aberto.
- **`AutorizacaoModalidade`:** Determina se o prestador possui autorização aprovada para a modalidade (`PESSOA` ou `ENCOMENDA`) da solicitação exibida.
- **`RegiaoAtuacao` / Endereços da Solicitação:** Suportam a filtragem por região (cidade/UF) e exibição da localização sem expor dados pessoais desnecessários do cliente.
- **Privacidade:** DTO de leitura expõe apenas informações operacionais necessárias à avaliação do serviço, omitindo dados pessoais sensíveis/desnecessários do cliente.

## 3. Fluxo e Contratos de Consulta
- **Endpoint de Consulta:** `GET /api/prestador/solicitacoes`.
- **Parâmetros de Filtro:**
  - Modalidade (`PESSOA`, `ENCOMENDA` ou `TODAS`).
  - Região (cidade / UF, opcional).
- **Comportamento Esperado:**
  1. Identifica o prestador autenticado e suas modalidades autorizadas.
  2. Busca apenas solicitações abertas e **sem direcionamento a outro prestador**.
  3. Aplica filtros de modalidade e região.
  4. Mapeia para DTO sem dados pessoais desnecessários do cliente e indica se o prestador está autorizado na modalidade da solicitação.

## 4. Interface Web (Prototipo Figma: `docs/interface/prestador-solicitacoes-disponiveis`)
- Utilizar o `.png` e o `.json` de `docs/interface/prestador-solicitacoes-disponiveis` como referência visual e estrutural exata (layout, filtros, cards/lista, modal/painel de detalhes e mensagem de vazio).
- Adaptar apenas nomes de campos, tipos, rotas e hooks de autenticação/API para o que já está implementado na branch `main`.
- Integrar com o fluxo de proposta existente (**RF007**) sem alterar a lógica do RF007.

## 5. Testes Automatizados (Somente no Backend)
1. **Testes Unitários:**
   - Retorno apenas de solicitações abertas e não direcionadas.
   - Aplicação correta dos filtros por modalidade e região.
   - Omissão de dados pessoais desnecessários do cliente no DTO de resposta.
   - Verificação de autorização do prestador por modalidade.
   - Retorno vazio quando não há correspondência aos filtros.
2. **Testes Sistêmicos com Selenium:**
   - Acessar o painel de solicitações disponíveis como prestador autenticado.
   - Filtrar solicitações por modalidade e região e conferir os itens exibidos.
   - Verificar que solicitações direcionadas a outro prestador não aparecem no painel aberto.
   - Verificar mensagem informativa quando o filtro não retorna resultados.
   - Verificar visualização dos detalhes da solicitação sem exposição de dados pessoais desnecessários do cliente.
