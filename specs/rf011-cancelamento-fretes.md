# RF011 — Cancelamento de Fretes

## Objetivo

Permitir que o cliente ou o prestador responsável cancele um frete
contratado antes do início da execução, mantendo o histórico e avisando
a outra parte.

## Referências

RF011, RN014 e RU005 do documento de requisitos do MUV.
Protótipo “Frete — Cancelar antes da execução”.

## Critérios de aceite

1. Dado um frete contratado que ainda não começou, quando o cliente
   ou o prestador responsável confirma o cancelamento com uma
   justificativa válida, o sistema muda a situação para Cancelado,
   registra autor, data, hora e justificativa e notifica a outra parte.
2. Se a justificativa estiver vazia ou contiver apenas espaços, o
   sistema rejeita a operação e mantém o frete como estava.
3. Se a execução já começou, inclusive se estiver interrompida,
   o sistema rejeita o cancelamento. A API aplica a mesma regra
   mesmo quando a tela já estava aberta antes do início.
4. Uma conta sem vínculo com o frete não pode cancelá-lo.
5. Um segundo envio para um frete já cancelado não duplica o registro
   nem a notificação.
6. O cancelamento não aplica multa, cobrança ou reembolso automático.

## Dependências e validação

- Usar os estados do frete contratado definidos pelo projeto.
- Coordenar com o RF010 a disputa entre início e cancelamento:
  somente uma transição pode prevalecer.
- Usar o mecanismo de notificações do RF013. A central de notificações
  não faz parte desta entrega.
- Validar com dois perfis autorizados, uma conta sem vínculo e fretes
  de teste antes e depois do início da execução.

## Tarefas

- [ ] Conferir o schema e os contratos existentes de frete, histórico
      e notificação na branch-base.
- [ ] Escrever testes para os critérios de aceite.
- [ ] Implementar a validação e o registro do cancelamento na API.
- [ ] Integrar a tela de justificativa e confirmação.
- [ ] Executar os testes e conferir o histórico no navegador.
