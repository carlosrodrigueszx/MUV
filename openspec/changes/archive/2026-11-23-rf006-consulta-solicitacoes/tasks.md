# Tarefas do RF006

## 1. Alinhamento com a `main` e Artefatos do Figma
- [x] **1.1.** Conferir classes, entidades, enums, segurança, endpoints e padrões já presentes na branch `main`.
- [x] **1.2.** Analisar `docs/interface/prestador-solicitacoes-disponiveis` (`.png` e `.json`) para mapear layout, componentes, textos, filtros, ações do protótipo e estado vazio.

## 2. Backend — Consulta e Filtragem
- [x] **2.1.** Implementar/ajustar consulta no repositório para buscar solicitações abertas, incluindo as direcionadas ao próprio prestador, excluindo as direcionadas a outros prestadores e aplicando filtros de modalidade e região.
- [x] **2.2.** Criar/ajustar DTO de consulta sem dados pessoais desnecessários do cliente e com o indicador de autorização do prestador na modalidade.
- [x] **2.3.** Implementar/ajustar serviço e controller de consulta de solicitações abertas para o prestador.

## 3. Frontend — Tela do Protótipo
- [x] **3.1.** Implementar a tela e componentes conforme `docs/interface/prestador-solicitacoes-disponiveis` (`.png` e `.json`), ajustando apenas integrações/tipos/rotas à branch `main`.
- [x] **3.2.** Implementar filtros por modalidade e região, visualização de detalhes da solicitação e mensagem informativa quando não houver resultados.
- [x] **3.3.** Implementar conexão com RF007, que mostra os detalhes da solicitação selecionada.

## 4. Testes no Backend (Unitários e Selenium)
- [x] **4.1.** Implementar testes unitários para a lógica de consulta, filtros, exclusão das direcionadas a outros prestadores, autorização por modalidade e ocultação de dados pessoais desnecessários.
- [x] **4.2.** Implementar testes sistêmicos com Selenium no backend cobrindo acesso ao painel, filtragem por modalidade/região, exclusão das direcionadas a outros prestadores e estado vazio.
- [x] **4.3.** Executar a suíte de testes do backend e validar o resultado.
## Validação em 23/11/2026

- Build do frontend aprovado. O Maven concluiu o `verify` do backend com 221 testes aprovados, zero falhas, zero erros e zero ignorados. A execução usou PostgreSQL no Docker, Selenium no Edge e os cenários opcionais de cancelamento habilitados (`-Dmuv.rf011.e2e=true`). Os seis testes da RF006 passaram, incluindo os dois sistêmicos.
- Após o ajuste visual do item 3.1, o build do frontend e os seis testes da RF006 passaram novamente. O Selenium verificou o painel do prestador, a filtragem, o estado vazio, a navegação à RF007 e a largura da página em janela estreita. As capturas de desktop e mobile ficam em `backend/target/rf006-evidencias/`.
