# Design

## Context

O documento oficial na branch `documents` vincula RF012 à US-013 e à RN016. O cartão da US-013 menciona o prestador, e seus critérios atribuem a conclusão ao motorista responsável. A V1 já define os campos de conclusão em `frete`, o evento `CONCLUSAO` e a tabela `notificacao`. O RF011 oferece uma referência de controle concorrente para o ciclo de vida.

## Goals / Non-Goals

- Integrar o registro e a consulta da conclusão ao fluxo de fretes contratados, com validação de autorização por operação.
- Compartilhar o estado concluído com RF014 e US-015. A gestão da contestação, o envio externo de notificações e a central de leitura pertencem às respectivas funcionalidades.

## Decisions

- **Identidade.** Reutilizar a sessão do RF001 e a resolução de identidade por `Principal` e `AuthRepository`. Autorizar a escrita pela conta `motorista.usuario_id` associada ao `frete.motorista_atual_id`. O autor vem da sessão. O cliente contratante é identificado pela solicitação vinculada. Validar vínculo também na leitura e manter dados do recebedor fora do histórico genérico e das notificações.
- **Transição.** Aplicar a regra de State para `EM_EXECUCAO → CONCLUIDO` no serviço de domínio. Seguir o RF011 com `JdbcClient`, bloqueio da linha de frete, releitura de estado e vínculo e atualização condicionada a `EM_EXECUCAO`. As operações concorrentes do ciclo de vida precisam compartilhar o bloqueio. Usar DTOs de entrada e saída e reaproveitar o tratamento de sessão, CSRF e erros de domínio da API.
- **Modalidade.** Aplicar as validações específicas pela modalidade persistida, usando Strategy. Persistir em `data_hora_conclusao` a data e o horário informados da conclusão do transporte de pessoas ou da entrega da encomenda. Capturar o instante do registro no servidor depois de adquirir o bloqueio e guardá-lo em `historico_execucao_frete.data_hora`. Verificar a apresentação de horários e a passagem de mês no RF014 antes de fechar o contrato da API.
- **Auditoria e notificação.** Acrescentar uma única entrada `CONCLUSAO` com autor e instante do registro. Usar a tabela interna e o destinatário contratante na mesma transação da conclusão, seguindo RF011. Se o Observer do RF013 estiver disponível na integração, reutilizar o mecanismo mantendo a garantia de gravação única e consistente.
- **Interface.** Usar os PNGs e JSONs das pastas `docs/interface/registrar-conclusao-frete/pessoas` e `docs/interface/registrar-conclusao-frete/encomendas` como referências do formulário. Ambas as páginas apresentam formulário e resumo do frete, com retorno ao acompanhamento e confirmação explícita do serviço realizado. Para pessoas, apresentar data e horário da conclusão. Para encomendas, apresentar data e horário da entrega, nome e CPF do recebedor. Ajustar o rótulo e o texto de ajuda para CPF conforme o modelo oficial, pois a referência contém um exemplo de RG. As duas telas incluem observações opcionais. Usar dados reais do frete no resumo e preservar a autorização pelo motorista designado, inclusive na navegação da área do prestador.
- **Consulta do cliente.** Detalhar a interface de consulta, os estados responsivos e os dados do recebedor visíveis por perfil antes de definir o DTO de leitura. Confirmar o ponto de entrada do motorista a partir do acompanhamento no fluxo integrado.

## Risks / Trade-offs

- RF010 ausente na base analisada → validar o contrato de transição com a equipe e usar fixtures de PostgreSQL com fretes em execução até a integração. O teste completo no navegador depende do fluxo integrado.
- Horário da entrega pode anteceder o registro → distinguir os dois no contrato e validar o recorte mensal do histórico.
- Outras contas podem ter acesso ao histórico via propostas → aplicar autorização própria aos detalhes sensíveis e testar o acesso de prestador sem vínculo com a conclusão.
- O rótulo atual do evento no RF014 é “Entrega concluída” → revisar a apresentação para pessoas com as referências da interface.

## Migration Plan

O esquema atual já oferece os campos e os tipos de evento necessários. Confirmar a aplicação das migrações existentes e o contrato temporal em PostgreSQL antes de propor qualquer migração adicional.
