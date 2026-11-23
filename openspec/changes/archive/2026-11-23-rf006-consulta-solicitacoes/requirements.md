# Requisitos — RF006: Consulta de Solicitações

## 1. Objetivo
O sistema deve disponibilizar um painel para que prestadores consultem solicitações abertas, utilizando filtros por modalidade de frete e região, permitindo a apresentação de propostas somente aos prestadores autorizados.

## 3. História de usuário
**US-007**: Como prestador de serviços de frete, quero apresentar contrapropostas para as propostas dos clientes.

## 4. Requisitos Não Funcionais Aplicáveis
- **RNF001 (Usabilidade e Responsividade):** Interface web responsiva para desktop e dispositivos móveis, seguindo o protótipo do Figma em `docs/interface/prestador-solicitacoes-disponiveis` (`.png` e `.json`).
- **RNF002 (Desempenho):** Consulta de solicitações abertas com desempenho adequado e sem consultas redundantes/N+1.
- **RNF003 (Segurança e LGPD):** Autenticação, autorização por perfil/permissão e proteção/omissão de dados pessoais desnecessários dos clientes.

## 5. Diretrizes de Implementação e Testes
- **Fidelidade ao Protótipo e à `main`:** Seguir o mais fielmente possível os arquivos `.png` e `.json` em `docs/interface/prestador-solicitacoes-disponiveis`, fazendo apenas os ajustes necessários para compatibilidade com o que já foi implementado pela equipe na branch `main`.
- **Escopo de Testes:** Implementar testes **unitários** e **sistêmicos (com Selenium)** exclusivamente no **backend**.

### Critérios de Aceitação
1. **Acesso ao Painel de Solicitações Abertas:** **QUANDO** o prestador acessar o painel de solicitações abertas, **O SISTEMA DEVE** exibir as solicitações que estão aguardando resposta.
2. **Filtro por Modalidade e Região:** **QUANDO** o prestador aplicar filtros de modalidade de frete e/ou região, **O SISTEMA DEVE** retornar apenas as solicitações abertas correspondentes.
3. **Privacidade e Proteção de Dados:** **ENQUANTO** consulta as solicitações no painel, **O SISTEMA NÃO DEVE** divulgar dados pessoais desnecessários dos clientes.
4. **Autorização por Modalidade:** **O SISTEMA DEVE** verificar as modalidades autorizadas do prestador para habilitar/bloquear ações restritas a prestadores autorizados naquela modalidade específica.
5. **Ausência de Resultados:** **QUANDO** não houver solicitações correspondentes aos filtros aplicados, **O SISTEMA DEVE** apresentar uma mensagem informativa.