# Design

## Context

See `proposal.md` and `specs/notificacoes-internas/spec.md` for motivation and behavior. The initial schema already contains `Notificacao` with recipient, title, message, creation time, unread boolean, nullable freight/request references, and a check requiring at least one reference. Existing proposal, hiring, designation, and cancellation flows insert rows directly. The backend uses Spring MVC/Security, PostgreSQL, Flyway, and JDBC (`JdbcClient`/`JdbcTemplate`); contracting already demonstrates synchronous Spring application events. The frontend uses React, React Router, and the existing session/CSRF API conventions. The supplied RF013 desktop capture establishes the inbox hierarchy, status filters, bulk read action, notification cards, and contextual actions; mobile references are still pending.

The current producer map includes `EnvioSolicitacaoService.enviar` (directed requests), `PropostaService.responder` (proposals/counterproposals), `ContratacaoService.aceitar` (confirmed hiring), `DesignacaoService` (designation, substitution, and agreement), and `FreightCancellationService.cancel` (pre-execution cancellation). RF010 now writes execution start, occurrences, interruption/resumption, and closure through `TransicaoFreteService.confirmar`; completion is written by `ConclusaoFreteService`. `FreightHistoryService` remains a read-only history consumer.

The observer is called only after new domain writes and, for RF010, after the event receipt is persisted. Idempotent request/proposal/acceptance/execution/completion retries return their original result before publishing again. The synchronous listener runs inside the originating transaction, so a persistence failure rolls back both the business event and its notification. Directed request creation now publishes through this same path; it notifies the selected responsible provider account. RF010 lifecycle notices go to the contracting customer and responsible provider account; completion goes to the contracting customer. Linked drivers do not receive a provider-side copy.

## Goals / Non-Goals

**Goals:**
- Centralize notification creation and recipient-scoped queries without changing the business outcome of existing workflows.
- Ensure each event's notification is persisted consistently with its originating domain transaction.
- Provide an authenticated inbox and read actions for the active account, with navigation to resources whose own APIs enforce authorization.
- Match the supplied RF013 visual reference and adapt it responsively when the remaining screens are provided.

**Non-Goals:**
- Push, email, SMS, or other external delivery channels.
- Replacing freight history as the source of event details or adding unrelated business rules to lifecycle flows.
- Exposing sensitive user data in notification text or making resource authorization depend on notification ownership alone.

## Decisions

### Use synchronous application events as the Observer boundary

Business services publish typed notification events only after the corresponding operation passes validation and writes its domain changes. A notification observer resolves/persists the notification through a dedicated service/repository. Keep the listener synchronous within the caller's transaction so failure rolls back the originating change and its notification together, matching the existing RF011 transaction behavior. This follows the project's existing `ApplicationEventPublisher` pattern; asynchronous listeners or an outbox would add delivery/recovery machinery not currently present and could allow a committed event without its notification.

Each event carries recipient account IDs and only the minimal resource IDs/context needed to compose an informative notification. Recipient selection remains in the domain flow that knows the parties and permissions; for company/cooperative freights, provider-side notices go to the responsible provider account and are not copied to an individually linked driver's account. The observer must not infer access from a company or driver name. Existing direct inserts are migrated to the shared notification path as each event producer is connected, preventing duplicate notices.

### Reuse the existing notification table first

Use the current `Notificacao` columns and constraints for the first implementation: `lida` supplies read state without adding a separate read timestamp, `criado_em` supplies ordering/time, and request/freight IDs provide navigation context. Preserve the check that one of those references exists. Do not create duplicate domain history or store unnecessary personal snapshots in the message. Add a Flyway migration only if implementation review identifies a required field not represented by this schema; do not renumber existing migrations.

### Scope inbox queries and mutations by the authenticated account

Expose an authenticated API under `/api/notificacoes` for listing/counting unread items and marking one or all as read. The account ID is taken from the authenticated session, never from a request parameter or body. Repository reads and updates include that account ID in their predicates. Return only display-safe notification fields plus resource IDs/kind needed for authorized navigation. Each destination endpoint continues to enforce its existing access checks.

Use the project's session and CSRF conventions for mutations. Listing may filter all/unread notifications as shown in the capture; the unread count and filter state are derived from the server response, not hard-coded sample values.

### Build the inbox as a focused frontend feature

Add a notification API client, types, and inbox page, then register a route and connect the bell/notification entry points already present in account/provider layouts. Render the supplied desktop composition: heading and explanatory text, all/unread filters, mark-all action, reverse-chronological cards, unread marker, timestamp, safe event context, and an action only when a supported destination can be resolved. Keep loading, empty, error, and action-pending states explicit. Apply responsive layout to the same content when mobile captures arrive; do not use sample names or IDs as production data.

## Risks / Trade-offs

- **[Several workflows already insert notifications directly]** A partial observer migration could create duplicates or omit events -> inventory all insert sites and migrate each covered event atomically; add event-to-recipient integration tests.
- **[Recipient rules for organizational drivers are not fully evidenced in the available source text]** A broad recipient rule could disclose freight activity -> retain explicit recipients from each authorized domain flow and confirm the driver/company matrix against the full RF013 specification before implementation.
- **[Current schema has only a boolean read flag]** It cannot report when an item was read -> use the existing supported read state unless the official RF013 contract requires a read timestamp; add an additive migration only then.
- **[Responsive reference set is incomplete]** The desktop layout alone may not settle mobile navigation/card actions -> finalize mobile-specific spacing and control placement after the user supplies those screens.
- **[Lifecycle producers span multiple active feature branches]** Integrating incorrectly could conflict with nearby changes -> check current Git/PR/worktree state again before implementation and preserve each flow's locking and transaction rules.
- **[RF010 requests can be replayed after a lost response]** A retry must not create another notice -> publish only on the new-event path after the operation receipt is persisted; replay paths return before publishing, and the observer remains synchronous inside the same transaction.

## Migration Plan

1. Before implementation, recheck branch/status/worktrees/PRs and reconcile the RF013 source from `origin/documents` with the recipient/event matrix and the supplied reference screens.
2. Implement the authenticated read API and central notification persistence path using the current table; add a migration only for a confirmed schema gap.
3. Connect each listed producer event, replacing its existing direct insert in the same scoped edit and transaction.
4. Add recipient-isolation, event-delivery, read-state, and API tests before connecting the React inbox.
5. Add the responsive inbox and authorized resource links; validate desktop/mobile states against the supplied screens.
6. Rollback application code without deleting persisted notifications. Any schema rollback must use a compensating migration and preserve existing records.

