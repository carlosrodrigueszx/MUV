# Requisitos — RF006: Consulta de Solicitações

## 1. Objetivo
O sistema deve disponibilizar um painel para que prestadores consultem solicitações abertas, utilizando filtros por modalidade de frete e região, permitindo a apresentação de propostas somente aos prestadores autorizados.

## 2. Escopo
**Como** prestador de serviços de frete (motorista autônomo ou empresa/cooperativa)[cite: 9, 18],  
**quero** consultar solicitações abertas de pessoas e encomendas filtrando por modalidade e região[cite: 9, 20],  
**para** encontrar oportunidades compatíveis com minhas autorizações[cite: 9, 20].

## 3. História de usuário
**US-006**: 

## 4. Requisitos Não Funcionais Aplicáveis
- **RNF001 (Usabilidade e Responsividade):** Interface web responsiva para desktop e dispositivos móveis, seguindo o protótipo do Figma em `docs/interface/prestador-solicitacoes-disponiveis` (`.png` e `.json`)[cite: 22].
- **RNF002 (Desempenho):** Consulta de solicitações abertas com desempenho adequado e sem consultas redundantes/N+1[cite: 22].
- **RNF003 (Segurança e LGPD):** Autenticação, autorização por perfil/permissão e proteção/omissão de dados pessoais desnecessários dos clientes[cite: 9, 19, 22].

## 5. Diretrizes de Implementação e Testes
- **Fidelidade ao Protótipo e à `main`:** Seguir o mais fielmente possível os arquivos `.png` e `.json` em `docs/interface/prestador-solicitacoes-disponiveis`, fazendo apenas os ajustes necessários para compatibilidade com o que já foi implementado pela equipe na branch `main`.
- **Escopo de Testes:** Implementar testes **unitários** e **sistêmicos (com Selenium)** exclusivamente no **backend**.

### Critérios de Aceitação
1. **Acesso ao Painel de Solicitações Abertas:** **QUANDO** o prestador acessar o painel de solicitações abertas, **O SISTEMA DEVE** exibir as solicitações com status aberto[cite: 9, 20].
2. **Exclusão de Solicitações Direcionadas:** **QUANDO** uma solicitação for direcionada a um prestador específico, **O SISTEMA NÃO DEVE** exibi-la no painel de solicitações abertas[cite: 9].
3. **Filtro por Modalidade e Região:** **QUANDO** o prestador aplicar filtros de modalidade de frete e/ou região, **O SISTEMA DEVE** retornar apenas as solicitações abertas correspondentes[cite: 9, 20].
4. **Informações para Avaliação do Serviço:** **QUANDO** o prestador consultar a listagem ou os detalhes de uma solicitação aberta, **O SISTEMA DEVE** exibir os dados necessários para avaliação conforme a modalidade:
   - Dados comuns (origem, destino, data/hora pretendida, modalidade)[cite: 7, 18, 24].
   - Modalidade de pessoas: quantidade de pessoas e informações sobre bagagens (quando aplicável)[cite: 7, 18, 24].
   - Modalidade de encomendas: quantidade/características dos volumes e informações necessárias para coleta e entrega[cite: 7, 18, 25].
5. **Privacidade e Proteção de Dados (RN017 / RNF003):** **ENQUANTO** consulta as solicitações no painel, **O SISTEMA NÃO DEVE** divulgar dados pessoais desnecessários dos clientes[cite: 9, 19, 22].
6. **Autorização por Modalidade (RN004):** **O SISTEMA DEVE** verificar as modalidades autorizadas do prestador para habilitar/bloquear ações restritas a prestadores autorizados naquela modalidade específica[cite: 9, 18, 20, 32].
7. **Ausência de Resultados:** **QUANDO** não houver solicitações correspondentes aos filtros aplicados, **O SISTEMA DEVE** apresentar uma mensagem informativa[cite: 9, 31, 32].