# RF009 — Designação de motoristas e veículos

## Why

Depois da contratação, o MUV precisa vincular ao frete os recursos que efetivamente executarão o serviço. A US-011, a RN011, a RN012 e a RN013 exigem que essa designação seja autorizada, compatível, rastreável e concluída antes do início da execução.

## What Changes

- Disponibilizar ao prestador responsável o contexto do frete contratado e os motoristas e veículos elegíveis para a designação.
- Validar no servidor propriedade, vínculo, verificação, atividade, modalidade e capacidade dos recursos selecionados.
- Registrar a designação de forma transacional, atualizar o frete para pronto para início e manter o evento correspondente no histórico.
- Permitir substituições que preservem as condições contratadas, com justificativa, histórico e notificação ao cliente.
- Impedir a aplicação imediata de substituições que alterem as condições; elas dependem de novo acordo com o cliente.
- Identificar o próprio prestador autônomo como motorista da execução e alinhar seu vínculo técnico ao modelo de motoristas.
- Permitir ao cliente consultar o motorista e o veículo atualmente designados para seu frete.
- Bloquear o início da execução enquanto a designação válida não estiver completa.
- Cobrir as regras com testes unitários, de API, de integração com PostgreSQL e de sistema no navegador.
- Organizar a entrega em duas etapas: primeiro o núcleo para organizações, baseado nos RF003 e RF004 já existentes; depois a integração obrigatória do motorista autônomo quando o RF002 fornecer sua representação definitiva.

## Capabilities

### New Capabilities

- `designacao-motoristas-veiculos`: cobre a consulta de recursos elegíveis, designação inicial, substituição, auditoria, notificação e consulta da designação de um frete contratado.

### Modified Capabilities

Nenhuma. A apresentação de propostas continua terminando antes da contratação; o RF009 consome o frete que será produzido pelo RF008.

## Impact

A mudança envolve o portal React do prestador, consulta da designação pelo cliente, endpoints Spring Boot, persistência PostgreSQL e histórico/notificações. Reutiliza a sessão atual, os motoristas vinculados do RF003, os veículos do RF004 e os dados de frete previstos no esquema. Depende funcionalmente do RF008 para que fretes reais sejam criados em `AGUARDANDO_DESIGNACAO`; até essa integração, os testes usarão dados controlados.

A primeira etapa não alterará o cadastro do autônomo nem criará representação provisória para ele. A integração correspondente permanecerá como tarefa pendente e obrigatória desta mudança até a implementação do RF002 na próxima Sprint; por isso, a mudança não poderá ser arquivada como concluída ao terminar apenas a etapa organizacional.

A implementação precisará coordenar migrações Flyway aditivas sem presumir capacidade volumétrica, categoria veicular ou agenda que ainda não existam no modelo. Alterações em roteamento, autenticação/cadastro e outros arquivos compartilhados deverão ser verificadas com os responsáveis antes da edição.
