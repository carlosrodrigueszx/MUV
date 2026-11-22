# Proposal

## Why

Objetos esquecidos nos veículos precisam de um registro que permita ao prestador localizar o item e devolvê-lo ao proprietário. O RF015 prevê esse fluxo, mas ele ainda não está descrito como uma capacidade no OpenSpec nem implementado na aplicação.

## What Changes

- Permitir que o prestador responsável ou um motorista vinculado a ele registre um objeto encontrado, incluindo foto, descrição, data e veículo.
- Permitir associar opcionalmente o registro a um frete, como apoio à identificação do proprietário.
- Permitir ao prestador responsável e aos seus motoristas vinculados consultar os registros dos veículos daquele prestador.
- Apoiar a identificação do proprietário sem exibir dados pessoais nem criar comunicação dentro do MUV; o contato ocorre fora da plataforma.
- Manter o item em guarda até a entrega ser confirmada; então atualizar sua situação para devolvido, sem exigir termo assinado.
- Restringir consultas e alterações ao contexto do prestador responsável pelo veículo.

## Capabilities

### New Capabilities

- `gestao-achados-perdidos`: registro, consulta e atualização da situação de objetos esquecidos em veículos, com acesso limitado ao prestador responsável e aos motoristas vinculados.

### Modified Capabilities

Nenhuma.

## Impact

A mudança envolve uma nova capacidade para os portais do prestador e do motorista, a API Spring Boot e a persistência PostgreSQL já configurada. O modelo inicial já prevê achados, fotos, veículo e usuário registrador; a proposta de RF015 acrescenta o vínculo opcional com frete e o acesso compartilhado dentro do prestador responsável. Os protótipos locais são referência visual e contêm elementos que não estão explicitamente definidos no requisito; esses elementos não serão tratados como regras sem decisão adicional.
