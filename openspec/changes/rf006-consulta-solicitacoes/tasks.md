# Tasks — RF006: Consulta de Solicitações

## 1. Alinhamento com a `main` e Artefatos do Figma
- [ ] **1.1.** Conferir classes, entidades, enums, segurança, endpoints e padrões já presentes na branch `main` (garantindo não alterar implementações existentes de outros requisitos, como RF007).
- [ ] **1.2.** Analisar `docs/interface/prestador-solicitacoes-disponiveis` (`.png` e `.json`) para mapear layout, componentes, textos, filtros e estado vazio.

## 2. Backend — Consulta e Filtragem (RF006)
- [ ] **2.1.** Implementar/ajustar consulta no repositório para buscar solicitações abertas, excluindo solicitações direcionadas e aplicando filtros de modalidade e região.
- [ ] **2.2.** Criar/ajustar DTO de consulta sem dados pessoais desnecessários do cliente (RN017 / RNF003) e com o indicador de autorização do prestador na modalidade.
- [ ] **2.3.** Implementar/ajustar serviço e controller de consulta de solicitações abertas para o prestador.

## 3. Frontend — Tela do Protótipo (RF006)
- [ ] **3.1.** Implementar a tela e componentes conforme `docs/interface/prestador-solicitacoes-disponiveis` (`.png` e `.json`), ajustando apenas integrações/tipos/rotas à branch `main`.
- [ ] **3.2.** Implementar filtros por modalidade e região, visualização de detalhes da solicitação e mensagem informativa quando não houver resultados.

## 4. Testes no Backend (Unitários e Selenium)
- [ ] **4.1.** Implementar testes unitários para a lógica de consulta, filtros, exclusão de direcionadas, autorização por modalidade e ocultação de dados pessoais desnecessários.
- [ ] **4.2.** Implementar testes sistêmicos com Selenium no backend cobrindo acesso ao painel, filtragem por modalidade/região, exclusão de direcionadas e estado vazio.
- [ ] **4.3.** Executar a suíte de testes do backend e validar o resultado.