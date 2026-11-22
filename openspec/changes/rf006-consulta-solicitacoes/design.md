# Design Técnico — RF006: Consulta de Solicitações

## 1. Visão Arquitetural
O desenho contempla exclusivamente a consulta e filtragem de solicitações abertas no painel do prestador (**RF006**), alinhado à arquitetura em camadas do MUV (Interface Web → Controllers → Serviços/Domínio → Repositórios → PostgreSQL)[cite: 38] e aos artefatos já existentes na branch `main`.

## 2. Domínio e Regras do RF006
- **`SolicitacaoFrete` (e especializações `SolicitacaoPessoa` / `SolicitacaoEncomenda`):** Considera solicitações em estado aberto e não direcionadas a prestador específico para exibição no painel aberto[cite: 9, 24, 25].
- **`AutorizacaoModalidade`:** Determina se o prestador possui autorização aprovada para a modalidade (`PESSOA` ou `ENCOMENDA`) da solicitação exibida[cite: 9, 24, 25].
- **`RegiaoAtuacao` / Endereços da Solicitação:** Suportam a filtragem por região (cidade/UF) e exibição da localização sem expor dados pessoais desnecessários do cliente[cite: 9, 24, 32].
- **Privacidade (RN017 / RNF003):** DTO de leitura expõe apenas informações operacionais necessárias à avaliação do serviço, omitindo dados pessoais sensíveis/desnecessários do cliente[cite: 9, 19, 22].

## 3. Fluxo e Contratos de Consulta (RF006)
- **Endpoint de Consulta:** `GET /api/prestador/solicitacoes-disponiveis` (ajustar prefixo/padrão de rota caso a `main` já utilize outro padrão nos controllers existentes).
- **Parâmetros de Filtro:**
  - Modalidade (`PESSOA` ou `ENCOMENDA`, opcional)[cite: 9, 24].
  - Região (cidade / UF, opcional)[cite: 9, 32].
- **Comportamento Esperado:**
  1. Identifica o prestador autenticado e suas modalidades autorizadas[cite: 20, 32].
  2. Busca apenas solicitações abertas e **sem direcionamento a outro prestador**[cite: 9, 32].
  3. Aplica filtros de modalidade e região[cite: 9, 20, 32].
  4. Mapeia para DTO sem dados pessoais desnecessários do cliente e indica se o prestador está autorizado na modalidade da solicitação[cite: 9, 22, 32].

## 4. Interface Web (Prototipo Figma: `docs/interface/prestador-solicitacoes-disponiveis`)
- Utilizar o `.png` e o `.json` de `docs/interface/prestador-solicitacoes-disponiveis` como referência visual e estrutural exata (layout, filtros, cards/lista, modal/painel de detalhes e mensagem de vazio)[cite: 9, 22].
- Adaptar apenas nomes de campos, tipos, rotas e hooks de autenticação/API para o que já está implementado na branch `main`.
- Integrar com o fluxo de proposta existente (**RF007**) sem alterar a lógica do RF007[cite: 20].

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
