# AGENTS.md — Projeto MUV

## 1. Contexto do projeto

O MUV é uma aplicação web voltada ao segmento de fretamento e aluguel de veículos de turismo, conectando as necessidades dos prestadores de serviços e dos usuários da plataforma.

O desenvolvimento deve seguir os requisitos funcionais, as regras de negócio, os protótipos do Figma e a arquitetura já estabelecida no repositório.

**Fontes de verdade, em ordem de prioridade:**
1. Requisitos e especificações oficiais do projeto.
2. Regras de negócio e modelos existentes na codebase.
3. Protótipos e definições de interface do Figma.
4. Arquitetura e padrões já estabelecidos no código.

Não invente funcionalidades, informações, regras de negócio ou comportamentos que não estejam especificados.

## 2. Tecnologias

### Frontend
- TypeScript
- React
- Vite
- Tailwind CSS v4
- React Router

### Backend
- Java
- Spring Boot

Respeite as versões, dependências, ferramentas de build e configurações já existentes no projeto. Não atualize versões ou introduza novas tecnologias sem necessidade justificada.

Identifique no repositório as ferramentas efetivamente utilizadas, incluindo banco de dados, ORM, gerenciador de dependências e configurações de execução. Não presuma tecnologias que não estejam configuradas.

## 3. Escopo de desenvolvimento

O desenvolvimento deve respeitar o escopo atual e os requisitos validados do MUV.

### Frontend
O frontend é responsável pela interface, navegação, apresentação dos dados e interações do usuário.

- Implemente somente funcionalidades previstas nos requisitos.
- Preserve a identidade visual e os padrões existentes.
- Reutilize componentes e estruturas já disponíveis.
- Evite refatorações gerais que não sejam necessárias para a tarefa.
- Não crie integrações ou dependências externas sem autorização.

### Backend
A implementação do backend está autorizada, neste momento, exclusivamente para a funcionalidade **Consulta de Prestadores**.

Essa autorização permite:
- Implementar os componentes necessários no Spring Boot.
- Disponibilizar os dados dos prestadores por meio de endpoints REST.
- Implementar regras de negócio relacionadas à consulta.
- Integrar a API à tela existente no frontend.
- Desenvolver os testes automatizados correspondentes.

Não implemente o backend de outros módulos ou funcionalidades sem autorização explícita.

## 4. Diretrizes de implementação

Antes de implementar qualquer funcionalidade:

1. Leia este arquivo integralmente.
2. Analise a estrutura da codebase.
3. Identifique os componentes, serviços, rotas e arquivos relacionados à tarefa.
4. Verifique se já existe alguma implementação que possa ser reutilizada.
5. Identifique dependências e possíveis impactos em outras partes do sistema.

Durante a implementação:

- Siga os padrões de nomenclatura e organização existentes.
- Prefira soluções simples, reutilizáveis e consistentes.
- Evite duplicação de código.
- Mantenha responsabilidades bem definidas.
- Não remova funcionalidades existentes sem autorização.
- Não substitua implementações existentes sem compreender suas dependências.
- Não introduza dados fictícios como se fossem dados reais.
- Não esconda erros com valores padrão ou tratamentos inadequados.

Se os requisitos forem insuficientes para uma decisão importante, apresente as dúvidas antes de implementar.

## 5. Integração com o Figma

Quando uma tarefa envolver implementação de interface a partir do Figma, utilize o Figma MCP sempre que suas ferramentas estiverem disponíveis.

Fluxo obrigatório:

1. Receba o link do frame ou da tela solicitada.
2. Utilize `get_design_context` para obter a estrutura e as informações do design.
3. Utilize `get_screenshot` para consultar a referência visual.
4. Confirme que os resultados correspondem ao frame solicitado.
5. Analise os componentes, espaçamentos, cores, tipografia, dimensões e demais propriedades relevantes.
6. Somente depois inicie a implementação.

Não comece a modificar arquivos antes de obter e analisar as referências necessárias.

Se alguma ferramenta do Figma MCP falhar ou não estiver disponível, informe o problema e solicite uma alternativa. Não invente informações sobre o design.

### Fidelidade visual

- Reproduza o protótipo com a maior fidelidade possível.
- Respeite cores, tipografia, espaçamentos, alinhamentos e dimensões.
- Observe os estados dos componentes e as interações previstas.
- Implemente responsividade sem descaracterizar o design.
- Reutilize componentes existentes quando forem compatíveis com o protótipo.
- Não crie elementos visuais que não estejam previstos no design ou nos requisitos.

## 6. Backend e API

Quando a tarefa estiver autorizada a envolver backend:

- Utilize Java e Spring Boot.
- Respeite a arquitetura existente.
- Separe controllers, serviços, repositórios e modelos conforme os padrões adotados.
- Utilize DTOs quando apropriado.
- Implemente validações e tratamento consistente de exceções.
- Respeite os contratos de comunicação entre frontend e backend.
- Utilize a persistência já configurada no projeto.
- Não crie uma arquitetura paralela.
- Não utilize listas estáticas ou dados simulados como substitutos da persistência real.

Antes de criar ou modificar entidades, endpoints e relacionamentos, consulte os requisitos e os modelos existentes.

Não altere o contrato de uma API sem verificar os impactos nos consumidores.

## 7. Testes e qualidade

Desenvolva testes compatíveis com a arquitetura e as ferramentas já utilizadas no projeto.

Para o backend, quando aplicável:
- JUnit 5 para testes automatizados.
- Mockito para isolamento de dependências.
- Spring Boot Test para testes de integração.
- MockMvc para testes de endpoints REST.

Para testes de sistema:
- Selenium WebDriver para automação no navegador.
- Validação dos fluxos reais entre frontend e backend.
- Verificação dos principais comportamentos e estados da interface.

Os testes devem utilizar dados controlados em ambiente apropriado, sem acessar ou modificar dados de produção.

Não considere um teste aprovado apenas porque foi escrito. Execute-o e informe o resultado real.

Antes de finalizar uma tarefa, verifique:
- Compilação e build.
- Testes relacionados à implementação.
- Funcionamento dos fluxos modificados.
- Possíveis regressões.
- Erros relevantes no console ou nos logs.

## 8. Git e proteção do trabalho em equipe

O projeto é desenvolvido por vários integrantes. Portanto, preservar o trabalho existente é obrigatório.

Antes de modificar arquivos, verifique:

- Branch atual.
- `git status`.
- Alterações locais não commitadas.
- Commits recentes.
- Worktrees existentes.
- Pull Requests pendentes que possam afetar os arquivos envolvidos.

### Regras obrigatórias

- Não sobrescreva alterações locais de outros integrantes.
- Não descarte alterações não commitadas.
- Não modifique arquivos envolvidos em PRs pendentes sem verificar seus impactos.
- Não altere arquivos protegidos por outro integrante sem autorização.
- Não faça commits, pushes, merges ou alterações em branches de terceiros sem autorização explícita.
- Não aprove, feche ou modifique Pull Requests sem autorização.
- Não remova worktrees sem autorização.

Não execute operações destrutivas que possam eliminar trabalho existente, incluindo:

- `git reset --hard`
- `git clean`
- `git restore` sobre alterações existentes
- `git checkout` para descartar alterações ou substituir arquivos

Se um arquivo necessário estiver envolvido em alterações de terceiros ou em um PR pendente:

1. Não modifique o arquivo.
2. Explique o conflito identificado.
3. Informe por que a alteração é necessária.
4. Solicite autorização antes de prosseguir.

Prefira implementar em arquivos independentes sempre que possível.

## 9. Organização e manutenção da codebase

- Preserve a estrutura de diretórios existente.
- Não crie arquivos ou pastas desnecessários.
- Evite componentes excessivamente grandes.
- Evite duplicação de lógica.
- Mantenha os componentes e serviços com responsabilidades claras.
- Não altere configurações globais sem necessidade.
- Não remova código aparentemente inutilizado sem verificar suas referências.
- Não faça refatorações fora do escopo solicitado.

Arquivos de roteamento, configurações globais e componentes compartilhados exigem atenção especial, pois podem afetar funcionalidades desenvolvidas por outros integrantes.

## 10. Processo de execução das tarefas

Para cada solicitação, siga este fluxo:

### Etapa 1 — Análise
- Leia este arquivo.
- Analise os requisitos.
- Inspecione a codebase.
- Verifique o estado do Git.
- Identifique arquivos envolvidos e possíveis conflitos.

### Etapa 2 — Planejamento
- Apresente uma breve descrição da solução.
- Identifique os arquivos que pretende criar ou modificar.
- Informe dependências e riscos.
- Sinalize dúvidas ou conflitos que impeçam a implementação.

Se houver risco de afetar trabalho de terceiros ou decisões arquiteturais importantes, aguarde autorização.

### Etapa 3 — Implementação
- Implemente somente o escopo solicitado.
- Preserve o código existente.
- Siga os padrões do projeto.
- Não realize alterações não relacionadas à tarefa.

### Etapa 4 — Validação
- Execute os testes pertinentes.
- Verifique o build.
- Valide os fluxos implementados.
- Analise possíveis regressões.

### Etapa 5 — Relatório final
Ao concluir, apresente:

1. Resumo do que foi implementado.
2. Arquivos criados e modificados.
3. Funcionalidades afetadas.
4. Testes executados e resultados.
5. Comandos necessários para executar ou validar a implementação.
6. Pendências, limitações e problemas encontrados.
7. Arquivos que não puderam ser modificados por conflitos ou proteção do trabalho de terceiros.

Não declare como concluída uma etapa que não foi efetivamente executada.

## 11. Regra principal

**Preserve a integridade do projeto e o trabalho de todos os integrantes.**

A prioridade é implementar corretamente os requisitos solicitados, respeitando a arquitetura existente, os protótipos, as regras de negócio e o estado atual do repositório.

Não tome decisões destrutivas, não invente requisitos e não amplie o escopo por iniciativa própria.