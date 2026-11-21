# Design do RF008

## Context

O RF007 usa Spring Boot, JdbcTemplate e PostgreSQL para persistir propostas como `ENVIADA`. Seus comandos bloqueiam a solicitação e verificam `ABERTA`. A V1 cria as tabelas necessárias, inclusive as duas restrições únicas do frete. O histórico já lê solicitações e fretes persistidos e reconhece `AGUARDANDO_DESIGNACAO` no filtro de contratados.

## Goals / Non-Goals

- Garantir os invariantes da RN010 e a gravação consistente do acordo e da notificação.
- Reutilizar autenticação por sessão, CSRF, persistência e os padrões de consulta do projeto.
- Consumir solicitações persistidas. A criação de solicitações, a central do RF013 e as transições do RF009 e RF010 ficam nos respectivos fluxos.

## Decisions

1. **Contrato de cliente.** Usar `GET /api/cliente/solicitacoes/{id}/propostas`, `POST /api/cliente/solicitacoes/{id}/propostas/{propostaId}/aceite` e `GET /api/cliente/fretes/{id}`. Separar do controller voltado ao prestador porque as regras de acesso diferem. Resolver o usuário autenticado pela sessão e filtrar pela titularidade em todas as operações. Usar 401 sem autenticação, 404 para recurso inexistente ou fora do escopo do titular e 409 para conflito de estado. O POST recebe os identificadores pela URL e retorna a referência do frete com 201 na criação ou 200 na repetição do aceite vencedor. O valor e as condições vêm do banco.
2. **Gravação transacional.** Bloquear a solicitação primeiro com `FOR UPDATE`, na mesma ordem do RF007. Após conferir titularidade, verificar se já existe frete para a solicitação e distinguir a repetição do aceite vencedor de outro aceite. No primeiro aceite, conferir solicitação `ABERTA` e proposta `ENVIADA` da mesma solicitação. Marcar a vencedora como `ACEITA` e a solicitação como `CONTRATADA`. Gravar o frete com os dados da proposta em `AGUARDANDO_DESIGNACAO`, com motorista e veículo vazios. Preservar as demais propostas e derivar a indisponibilidade do aceite da situação da solicitação. Uma atualização condicional também poderia arbitrar o aceite, mas o lock explícito permite reusar a ordem do RF007 e distinguir os desfechos antes de gravar.
3. **Integridade no banco.** Reutilizar `UNIQUE (solicitacao_id)` e `UNIQUE (proposta_aceita_id)` de `frete`. Usar os dados da mesma proposta para preencher os três vínculos. A FK composta reforçaria essa associação em gravações externas, com custo de índice adicional e verificação de dados históricos. Priorizar as constraints existentes e validar a associação na escrita transacional.
4. **Notificação.** Publicar um evento interno síncrono com o prestador, o frete e a solicitação e gravar a notificação em um listener que participe da transação. A falha deve propagar e causar rollback. Uma chamada direta seria menor, mas o listener segue o Observer previsto na arquitetura. A alternativa assíncrona exigiria entrega durável e retentativa.
5. **Frontend.** Usar Minhas viagens como entrada provisória, com navegação por IDs persistidos e consultas que funcionem após recarregar. Confirmar posição dos links, rotas e estados visuais com os frames. O aceite é comum a PESSOA e ENCOMENDA. Os DTOs de leitura exibem os dados pertinentes à modalidade conforme os frames e limitam o perfil do prestador aos dados públicos. Introduzir Strategy apenas se houver validações específicas documentadas para este fluxo.

## Risks / Trade-offs

- O lock serializa operações da mesma solicitação. Manter a transação curta e testar a ordem com o envio do RF007 e com os fluxos de cancelamento aplicáveis.
- A branch remota local do RF011 altera o router e contém uma V6 que se sobrepõe à numeração do RF004 e à área de notificações do RF007. Conferir integrações e PRs antes de editar arquivos compartilhados ou atribuir a versão da migration.
- A tela de planejamento atual navega para busca de prestadores com estado em memória. Validar o RF008 com solicitações persistidas via ambiente de teste e integrar o ponto de entrada real após conferir os frames.
- Antes da implementação, obter a autorização explícita para backend exigida pelo AGENTS.md. A implementação visual depende da análise dos frames solicitados via Figma MCP ou de uma alternativa fornecida pelo usuário.

## Migration Plan

Adicionar uma migration incremental para gerar IDs de frete pelo PostgreSQL, inicializando a sequência com base no maior ID existente. Verificar primeiro se uma integração recente já cobre essa geração. Testar a partir do esquema atual, inclusive com fretes preexistentes. Em rollback da aplicação, preservar a migration aditiva e os acordos já registrados.

## Referências conferidas durante o apply

- O backend do RF008 e a edição do router foram autorizados na conversa. A consulta ao GitHub retornou zero PRs abertos. A sobreposição da referência local do RF011 permanece um ponto de revisão quando as branches forem integradas.
- Foram conferidos os PNGs e os dados estruturais exportados em `docs/interface/cliente-comparar-propostas` (frame 489-629) e `docs/interface/confirmar-contratacao` (frame 496-634). A tela de confirmação também é o detalhe consultável. Meus fretes abre o histórico existente, que dá acesso à comparação e ao detalhe.
- O protótipo apresenta prazos em campos separados, mas a proposta persistida tem valor e condições textuais. A página mostra essas condições e a data disponível na solicitação. A indicação “Encerrada” após o aceite é derivada da contratação.
