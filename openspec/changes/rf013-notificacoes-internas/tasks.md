# Tasks

## 1. Domain and persistence

- [x] 1.1 Map each RF013 event to its existing command/service, recipient account, and resource IDs; verify organizational provider-side recipients are the responsible provider account only.
- [x] 1.2 Implement shared notification creation and synchronous Observer handling using the existing `Notificacao` schema; verify tests cover recipient, linked resources, unread state, and rollback on failed source operations.
- [x] 1.3 Connect directed-request, proposal/counterproposal, and accepted-contract events, replacing existing direct inserts where present; verify integration tests assert one notification per successful event and none for rejected or duplicate operations.
- [x] 1.4 Connect designation/agreement, execution/history, pre-execution cancellation, and completion events to the observer; verify each available producer test checks the intended customer/provider recipient and linked freight.
- [x] 1.5 Check whether any RF013 field is missing from the existing schema before adding a Flyway migration; if a migration is required, verify fresh and existing schemas preserve notification IDs, constraints, and data.

## 2. Authenticated notification API

- [x] 2.1 Add repository queries for all/unread notification lists and unread counts, always scoped by authenticated recipient; verify PostgreSQL integration tests exclude other users' rows and return only display-safe context.
- [x] 2.2 Add authenticated endpoints to list notifications and mark one or all as read, deriving the recipient exclusively from the session; verify MockMvc covers unauthenticated requests, CSRF, ownership isolation, and read-state updates.
- [x] 2.3 Define notification response DTOs with safe request/freight identifiers and supported navigation context; verify serialization omits private fields and links still rely on destination authorization.

## 3. Notification inbox UI

- [x] 3.1 Add the frontend notification API client and types using existing session/CSRF conventions; verify API error and authentication behavior is surfaced to the page.
- [x] 3.2 Build the responsive inbox route and connect existing notification entry points; verify the desktop layout against the supplied RF013 capture and review mobile details against the mobile references when supplied by the user.
- [x] 3.3 Implement all/unread filters, unread count, notification cards, individual/bulk read actions, and authorized context links; verify each action refreshes the server-backed state without hard-coded sample data.
- [x] 3.4 Implement loading, empty, error, and pending-action states; verify each state renders accessibly at desktop and mobile widths.

## 4. End-to-end verification

- [x] 4.1 Run the relevant backend tests and frontend build, then verify the key event-to-inbox flows against a controlled database and authenticated sessions.
- [x] 4.2 Recheck authorization and personal-data exposure for customer, autonomous provider, company/cooperative provider, and linked driver accounts; verify linked drivers cannot list or mutate notifications addressed to the responsible provider.
- [x] 4.3 Review the final Git diff and OpenSpec validation output; verify no unrelated files, existing changes, or other active OpenSpec changes were modified.
