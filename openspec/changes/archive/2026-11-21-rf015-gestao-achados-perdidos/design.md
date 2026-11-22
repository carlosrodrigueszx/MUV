# Design

## Context

Veja `proposal.md` para a motivação e `specs/gestao-achados-perdidos/spec.md` para o contrato de comportamento.

A migração inicial já contém `Achado_Perdido` com descrição, data, situação, veículo e usuário registrador, e `Foto_Item_Perdido` com URL da imagem. O registro ainda não possui vínculo com `Frete`, e nenhuma das duas tabelas tem sequência/default para novos IDs. O veículo pertence a um prestador, e o domínio possui vínculos entre motoristas e prestadores. O cadastro de motorista já recebe data URI, valida imagem e persiste bytes e tipo MIME em colunas `BYTEA`/`VARCHAR`; não foi encontrada implementação da feature de achados.

## Goals / Non-Goals

**Goals:**

- Reutilizar as tabelas existentes, completar a persistência de foto e IDs e adicionar o vínculo opcional do achado com um frete.
- Aplicar autorização no servidor com base no prestador responsável pelo veículo e no vínculo do motorista com esse prestador.
- Manter o contato com o proprietário fora do MUV e sem expor dados pessoais do passageiro.
- Atualizar a situação do item quando um usuário autorizado confirmar a entrega.

**Non-Goals:**

- Criar comunicação interna com o proprietário, exibir telefone, CPF ou outros dados pessoais, ou registrar termo assinado.
- Introduzir protocolo ARCE, métricas, linha/rota, localização do objeto ou local de custódia como regras de domínio nesta mudança; esses elementos aparecem nos protótipos, mas não foram confirmados no RF015 nem nas decisões tomadas.
- Adicionar serviço ou dependência externa para armazenar imagens sem decisão e autorização específicas.

## Decisions

### Autorização pelo contexto do veículo

O servidor deve resolver o prestador responsável a partir do veículo do registro. O prestador desse veículo e os motoristas vinculados a ele podem consultar e operar o registro. A mesma regra deve ser aplicada à criação, consulta individual, listagem e confirmação de entrega, para impedir acesso cruzado entre prestadores. A identidade do registrador continua associada ao item.

Essa regra foi escolhida porque cobre os dois portais dos protótipos sem ampliar a visibilidade para todos os usuários autenticados. A autorização não deve depender apenas de esconder ações na interface.

### Vínculo opcional com frete

Adicionar uma referência anulável ao frete, validada para pertencer ao mesmo prestador responsável pelo veículo. O frete serve como contexto para ajudar o prestador a identificar o possível proprietário; não é obrigatório para registrar o objeto e não concede acesso a dados pessoais do passageiro.

O vínculo deve ser aditivo para preservar achados já existentes. A alternativa de tornar o frete obrigatório foi rejeitada porque a equipe confirmou que o registro deve continuar possível quando ele não for conhecido.

### Situação e confirmação da entrega

Usar as situações `EM_GUARDA` e `DEVOLVIDO`, compatibilizando-as com os valores aceitos pelo esquema atual antes de implementar. A criação inicia em `EM_GUARDA`; a confirmação da entrega atualiza para `DEVOLVIDO`. Não criar fluxo de assinatura nem exigir identificação pessoal de quem recebeu.

Essa opção atende ao RF015 e à decisão do usuário de confirmar a entrega sem termo assinado. Campos adicionais de recebedor ou comprovante não serão deduzidos dos exemplos visuais.

Registros legados em `EM_CUSTODIA` permanecem com seu valor armazenado para preservar o histórico. A interface os apresenta como “Em guarda” e permite a confirmação de devolução, que os atualiza para `DEVOLVIDO`.

### Fotos e identificadores

Reutilizar `Foto_Item_Perdido` como relação entre achado e imagem e o padrão já usado em `Motorista`: o frontend envia data URI; o backend valida tipo PNG/JPEG/GIF, conteúdo real, tamanho máximo de 1 MB e dimensões máximas de 4096 px; o PostgreSQL armazena bytes em `BYTEA` e tipo MIME. A migração torna `url` anulável para preservar referências antigas e adiciona conteúdo/tipo com uma restrição que exige exatamente uma representação válida (URL antiga ou bytes novos). Nenhum serviço externo será introduzido.

A mesma migração cria sequências vinculadas às colunas de ID de `Achado_Perdido` e `Foto_Item_Perdido`, inicializadas acima dos registros existentes, porque a migração inicial não define defaults para essas tabelas.

## Risks / Trade-offs

- **[Compatibilidade com fotos existentes]** `Foto_Item_Perdido` já armazena URL e pode conter registros legados. **Mitigação:** manter URLs válidas, aceitar bytes para novos uploads e testar ambos os formatos após a migração.
- **[Frete opcional não identifica todos os proprietários]** Um objeto sem frete conhecido, ou um frete com vários passageiros, pode exigir identificação operacional. **Mitigação:** manter o contato fora do MUV, conforme decidido, sem revelar dados pessoais ou inventar associação automática.
- **[Status atual pode divergir dos rótulos do requisito]** A coluna existente é texto e os valores aceitos precisam ser conferidos nos dados e restrições atuais. **Mitigação:** verificar compatibilidade antes de gravar novos estados e preservar registros existentes por migração aditiva.

## Migration Plan

1. Conferir o estado Git e as migrações existentes antes de editar; preservar alterações locais e arquivos envolvidos em PRs de terceiros.
2. Verificar vínculos, valores e restrições de `Achado_Perdido`, o padrão de imagem já usado pelo cadastro de motorista e regras de sessão/autorização de prestador e motorista.
3. Implementar migração aditiva para IDs, conteúdo binário/tipo MIME da foto e vínculo opcional com frete, preservando URLs e registros existentes.
4. Implementar API e interface nos módulos próprios, integrando rotas compartilhadas de forma aditiva após conferir conflitos com trabalho paralelo.
5. Validar criação, listagem, autorização por prestador, associação opcional, transição para devolvido, persistência e comportamento dos portais.
6. Em reversão, remover pontos de entrada da feature e manter os registros existentes; alterações de esquema devem usar migração compensatória, sem apagar dados.
