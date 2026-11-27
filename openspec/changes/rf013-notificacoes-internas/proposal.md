# Proposal

## Why

The platform already records notifications in several individual business flows, but users cannot consult a unified, private inbox or manage read state. RF013 brings relevant request, proposal, hiring, assignment, execution, cancellation, and completion updates together so each involved user can follow activity they are authorized to see.

## What Changes

- Define the notification lifecycle, event coverage, recipient rules, privacy boundaries, and inbox behavior.
- Add authenticated notification listing and read-state actions scoped to the current user.
- Route domain events through a notification observer while keeping notification persistence consistent with the originating transaction.
- Connect existing and newly covered freight lifecycle events to internal notifications.
- Add a responsive inbox with read filters, bulk read action, contextual navigation, and loading, empty, and error states.

## Capabilities

### New Capabilities
- `notificacoes-internas`: Persist, deliver, list, and manage the read state of private notifications about authorized request and freight activity.

### Modified Capabilities

## Impact

- Backend: existing notification table and direct inserts in proposal, contracting, designation, and cancellation flows; notification query/write services, repository, controller, DTOs, security, and event listeners.
- Frontend: React Router and a new notification feature using the existing API/authentication conventions and the RF013 reference capture supplied by the user.
- Database: assess the current `Notificacao` schema and existing ID defaults before proposing any additive Flyway migration; preserve existing records.
- Tests: cover event delivery, persistence links, read actions, and recipient isolation using the repository's current Spring/JUnit/PostgreSQL patterns.
