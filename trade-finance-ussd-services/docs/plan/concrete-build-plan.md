# Trade Finance USSD Concrete Build Plan

## Source inputs

- `docs/plan/plan0.md`
- `docs/plan/PROJECT_SUMMARY.md`
- current implementation analysis
- Infobip USSD gateway spec
- flow docs: `journey-after-registration.png`, `registration-initial-loan-application.png`

## Goal

Build a production-ready Trade Finance USSD service that:

- routes users through registration vs post-registration journeys
- integrates with `profile-service`, `public-auth-service`, and `trade-finance-service`
- supports anchors, eligibility, loan application initiation, and guided USSD transitions
- keeps menu definitions maintainable and versioned

## Architecture decision

### Menus: database, not JSON

Use **database-backed menus** as the source of truth.

Why:

- menus change without redeploys
- transitions can be versioned and audited
- external services can enrich the context while the journey stays stable
- easier to support branching, rollback, and future admin tooling

Use JSON only as:

- a seed/export format
- local mock fixtures for tests
- a migration source if needed

### State machine

Use a **state machine / journey engine**.

Each request should:

1. load session state
2. resolve the current menu node
3. validate user input
4. apply transition rules
5. render the next screen
6. persist the new state

This is the right fit for USSD because the flow is sessionful, branching, and input-driven.

## Journey split

### 1. Registration initial loan application

Used when the msisdn is not found in profile.

Main path:

- check profile existence
- collect registration data
- capture PIN
- show registration success
- continue to anchor selection
- show loan offer
- capture amount
- confirm submission

### 2. Journey after registration

Used when the msisdn exists in profile.

Main path:

- request PIN
- validate PIN
- show welcome home
- select anchor
- show offer
- capture amount
- confirm application

## External service integration scope

### Profile service

Use for:

- profile existence check
- registration steps
- PIN/OTP-related validation
- account/profile enrichment

### Public auth service

Use for:

- authentication/token flow for downstream calls
- secured service access

### Trade finance service

Use for:

- anchor lookup
- eligibility/limit context
- loan application preparation

## Build phases

### Phase 1: Foundation

Deliverables:

- USSD gateway endpoints
- session state storage
- basic error handling
- menu engine skeleton

### Phase 2: Menu engine

Deliverables:

- DB schema for flows, nodes, options, transitions
- seeded journeys from the docs
- prompt templating
- back/next transition handling

### Phase 3: Registration and login

Deliverables:

- profile lookup integration
- registration flow
- PIN validation
- user context enrichment

### Phase 4: Trade finance journey

Deliverables:

- anchor lookup
- offer rendering
- amount capture
- confirmation step

### Phase 5: Hardening

Deliverables:

- logging and traceability
- retry/timeout handling
- integration tests
- deployment readiness

## User stories

### Epic A: USSD session foundation

- start session
- respond to session input
- end session cleanly

### Epic B: Menu engine

- render menus from stored definitions
- handle transitions
- support back navigation
- support validation rules

### Epic C: Registration and login

- determine profile existence
- route to the correct journey
- validate PIN/OTP
- collect registration data

### Epic D: Trade finance

- show anchors
- present eligible offers
- capture amount
- confirm application

### Epic E: Platform hardening

- observability
- error recovery
- test coverage
- rollout support

## Concrete milestone plan

### Milestone 1

- gateway contract
- session persistence
- static/mock journey engine

### Milestone 2

- DB-backed menus
- seeded flow data
- state machine transitions

### Milestone 3

- profile-service integration
- registration/login branches

### Milestone 4

- trade-finance-service integration
- anchor and offer flow

### Milestone 5

- validation, logging, testing, release readiness

## Menu model summary

Store:

- `flow`
- `node`
- `option`
- `transition`
- `action` (optional)

Each menu node should know:

- prompt template
- allowed inputs
- next node
- whether it closes the session
- whether it is terminal

## Recommended implementation order

1. keep the current static/mock journey engine working
2. switch the menu catalog to DB-backed lookup
3. seed the two journeys from the docs
4. integrate profile and auth services
5. integrate trade finance anchor/offer flow
6. harden and test end-to-end

