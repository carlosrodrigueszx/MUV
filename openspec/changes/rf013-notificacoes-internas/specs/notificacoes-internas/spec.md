# Spec Delta

## Purpose

Give each authorized MUV account a private, persistent inbox of relevant request and freight events, with controls to consult and manage notification read state.

## ADDED Requirements

### Requirement: Notify users about request and proposal events
The system SHALL create an internal notification for the authorized recipient when a directed freight request, a proposal or counterproposal, or a confirmed hiring changes that recipient's activity.

#### Scenario: Directed request
- **WHEN** a freight request is successfully directed to a provider
- **THEN** the directed provider receives a notification linked to that request.

#### Scenario: Proposal received
- **WHEN** a provider's proposal or justified counterproposal is successfully recorded for an open request
- **THEN** the customer who owns that request receives a notification linked to it.

#### Scenario: Hiring confirmed
- **WHEN** the customer accepts an eligible proposal and the freight is confirmed
- **THEN** the selected provider receives a notification linked to the request and the freight.

### Requirement: Notify users about freight lifecycle events
The system SHALL notify the customer and provider-side account(s) specified below about material assignment, agreement, execution, occurrence, cancellation, and completion events. For company or cooperative freights, a provider-side notification SHALL be addressed to the responsible provider account and SHALL NOT be copied to an individually linked driver account.

#### Scenario: Assignment or substitution
- **WHEN** an initial designation, resource substitution, or agreed service change is successfully registered
- **THEN** the customer receives a notification linked to the freight that describes the relevant change without exposing data outside their access.

#### Scenario: Execution event
- **WHEN** an execution start, relevant occurrence, interruption, resumption, or closure without completion is successfully recorded
- **THEN** the customer and responsible provider account receive a notification linked to the freight; a linked driver account does not receive a separate provider-side copy.

#### Scenario: Pre-execution cancellation
- **WHEN** the customer or responsible provider successfully cancels an eligible freight before execution
- **THEN** the other involved party receives a notification linked to the freight.

#### Scenario: Freight completion
- **WHEN** completion of a passenger or parcel freight is successfully recorded
- **THEN** the contracting customer receives a notification linked to the freight.

### Requirement: Persist notification context and read state
Each notification SHALL identify one recipient, creation time, read state, and at least one related freight or request, while preserving the domain event context needed by the recipient.

#### Scenario: Notification is created
- **WHEN** a covered event is successfully committed
- **THEN** its notification is stored as unread for the intended recipient and references the related request, freight, or both when available.

#### Scenario: Event is rejected
- **WHEN** a business operation is rejected and its domain event is not committed
- **THEN** no notification for that unsuccessful event is visible to recipients.

### Requirement: Consult only authorized notifications
The system SHALL return only notifications and related context that the authenticated account is authorized to access, and SHALL omit unnecessary personal or sensitive data about other users.

#### Scenario: List own notifications
- **WHEN** an authenticated user opens the notification inbox
- **THEN** the system returns only notifications addressed to that user and only the contextual fields permitted for that user.

#### Scenario: Attempt to access another user's notification
- **WHEN** a user attempts to list, retrieve, or change the read state of a notification addressed to another user
- **THEN** the system does not disclose the notification or change its state.

#### Scenario: Open related resource
- **WHEN** a user follows a notification's related-resource action
- **THEN** the destination resource applies its own authorization rules before returning protected data.

### Requirement: Manage notification read state
The system SHALL let an authenticated recipient mark an individual notification or all of their notifications as read.

#### Scenario: Mark one notification as read
- **WHEN** the recipient marks one of their unread notifications as read
- **THEN** the notification is recorded as read and remains associated with its original event.

#### Scenario: Mark all notifications as read
- **WHEN** the recipient selects the action to mark all notifications as read
- **THEN** all notifications addressed to that recipient are recorded as read and no other user's notifications are changed.

#### Scenario: Empty inbox
- **WHEN** the authenticated user has no notifications matching the selected read-state filter
- **THEN** the inbox presents an empty state without implying that an error occurred.
