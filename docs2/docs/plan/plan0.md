# Trade Finance USSD project analysis

## Context reviewed

- `docs/Infobip-USSD-Gateway_REST-JSON-API.pdf`
- `docs/journey-after-registration.png`
- `docs/registration-initial-loan-application.png`
- current Spring Boot skeleton and config in `src/main/resources`

## What the Infobip flow means for the app

- The gateway calls three HTTP endpoints: `start`, `response`, and `end`.
- The app owns all state and journey logic; Infobip only forwards requests.
- Each request is session-scoped via `sessionId`, so session state must be persisted outside request memory.
- Response payloads are simple: `shouldClose`, `ussdMenu`, `responseExitCode`, `responseMessage`.
- Menu text should be newline-delimited and kept short enough for USSD screens.

## Recommended architecture

### Journey engine

Use a database-backed journey engine for menu definitions, transitions, and conditions.

Why:

- dynamic menu items can be enabled/disabled without redeploying
- journey branching can change per product, country, customer segment, or registration state
- menu labels, prompts, and transitions can be managed by ops/config teams
- auditability is better than hard-coded JSON once the journey grows

### JSON vs database

| Option | Best for | Limits |
| --- | --- | --- |
| JSON menu config | early prototype, static menus | hard to change at runtime, weak audit/history, poor branching support |
| Database menus | dynamic menus, branching, feature flags, versioning | needs schema and admin/update path |

### Best fit for this project

Start with **database-backed menu definitions**, optionally seed them from JSON for the first release.

Use Redis for short-lived session/cache data only; keep the source of truth in PostgreSQL.

## External service responsibilities

- `profile-service`: registration steps, login/profile lookup, OTP/PIN validation, account linking
- `public-auth-service`: client authentication/token exchange
- `trade-finance-service`: anchor lookup and trade-finance domain data

## Suggested domain modules

1. USSD gateway adapter
2. Session state manager
3. Menu/transition engine
4. Profile-service client
5. Auth-service client
6. Trade-finance-service client
7. Journey analytics/audit logging

## User story breakdown

### Epic 1: USSD gateway integration

- As the gateway, I can start a session and receive the first menu.
- As the gateway, I can send user replies and receive the next menu.
- As the gateway, I can end a session cleanly.

Tasks:

- create request/response DTOs for Infobip payloads
- map `sessionId` to persisted session records
- return consistent exit codes and error messages

### Epic 2: Session and journey state

- As the system, I can store where the user is in the journey.
- As the system, I can resume a journey after each user input.
- As the system, I can expire abandoned sessions safely.

Tasks:

- design session table/entity
- design journey step/state model
- add Redis cache for active session context if needed

### Epic 3: Dynamic menu management

- As an admin, I can create and update menus without code changes.
- As an admin, I can define menu transitions and conditions.
- As an admin, I can version menus per journey or product.

Tasks:

- design menu, menu item, and transition entities
- support conditional transitions
- support enable/disable and versioning

### Epic 4: Registration and login flows

- As a customer, I can register through USSD.
- As a customer, I can log in or verify my profile.
- As a customer, I can complete OTP/PIN-based validation where required.

Tasks:

- integrate profile-service endpoints for onboarding
- map registration steps to USSD screens
- persist partially completed registration state

### Epic 5: Trade finance journey

- As a customer, I can view available anchors.
- As a customer, I can start an initial loan application.
- As a customer, I can progress through a guided loan journey.

Tasks:

- integrate trade-finance-service anchor lookup
- build loan application step flow
- add validation for required fields and eligibility

### Epic 6: Security and resilience

- As the platform, I can authenticate downstream service calls.
- As the platform, I can handle timeouts and retries safely.
- As the platform, I can log traceable journey events.

Tasks:

- configure auth token handling
- add timeout/retry policy per client
- add structured logging and correlation IDs

### Epic 7: Release readiness

- As the team, we can test the end-to-end journey.
- As the team, we can deploy safely to UAT and production.

Tasks:

- add integration tests for USSD request flows
- add contract tests for external services
- prepare environment-specific config and secrets handling

## Achievable milestones

### Milestone 1: Foundation

- project structure
- Infobip request/response DTOs
- session persistence
- basic health and config wiring

### Milestone 2: Journey engine

- database menu model
- transition rules
- dynamic menu rendering
- session resume logic

### Milestone 3: Registration flow

- profile-service integration
- onboarding screens
- OTP/PIN validation
- registration completion state

### Milestone 4: Login and authenticated access

- public-auth-service integration
- authenticated journey branches
- account/profile checks

### Milestone 5: Trade finance features

- anchor listing
- loan application flow
- review/confirm/submit journey

### Milestone 6: Hardening and rollout

- observability
- error handling
- integration testing
- UAT fixes

## Practical implementation choice

For this project, the cleanest approach is:

1. store journeys and transitions in PostgreSQL
2. cache active session state in Redis
3. keep a JSON seed for initial menu setup only
4. drive the USSD flow from a journey engine, not hard-coded controller branching

## Menu design approach for this project

Menus should be treated as **stateful screens** with:

- a stable `nodeId`
- a prompt template with placeholders like `{{name}}`, `{{currency}}`, `{{amount}}`
- optional backend action before rendering the next screen
- allowed user inputs and mapped next states
- close/continue behavior per screen

### Example menu sequence

1. `welcome-pin`
    - `Hi {{name}}. Please enter your PIN to proceed.`
    - validates PIN against `profile-service`
    - on success, moves to `welcome-home`

2. `welcome-home`
    - `Hi {{name}}, welcome to DTB.\n1. Select anchor\n2. My offers\n3. Exit`
    - fetches available anchors from `trade-finance-service`

3. `anchor-list`
    - `Select an anchor:\n1. {{anchor1}}\n2. {{anchor2}}...`
    - each selection routes to a qualifying / offer screen

4. `offer-summary`
    - `You qualify for up to {{currency}} {{amount}} from {{anchor}}`
    - can move to loan application or back

5. `amount-range`
    - `The amount must be between {{currency}} {{minAmount}} and {{currency}} {{maxAmount}}`
    - validates the entered amount before continuing

### How to model dynamic menus

Use three layers:

- **template**: menu text with placeholders
- **transition**: how input maps to next node
- **context**: runtime values from profile, anchors, and eligibility rules

This keeps the UI text separate from business logic and makes it easy to reuse menu nodes across journeys.

### Recommended rule pattern

- If the menu is informational only, close after rendering.
- If the menu expects input, store the node and validate the next reply against its rule set.
- If a backend call fails, return a graceful retry/error screen instead of advancing the journey.
- If the user selects back/cancel, resolve the previous node from session history.

## Project Architecture

### Core Components
- **USSD Session Manager**: Manages conversation state across multiple USSD exchanges
- **Infobip Gateway Client**: REST client for gateway integration
- **Menu Engine**: Flexible menu building system for complex flows
- **State Machine**: Tracks conversation flow and prevents invalid transitions

### look into the first three user stories for milestone 1
1. **US-001**: Setup USSD Session Management (M1)
2. **US-002**: Implement Infobip Gateway Client (M1)
3. **US-003**: Design Database Schema (M1)

## Database and migration design for menus

Model menus as versioned journey nodes:

- `ussd_menu_flow` - one journey or product flow
- `ussd_menu_node` - one screen/state in the flow
- `ussd_menu_option` - one selectable input on a node
- `ussd_menu_transition` - maps input or condition to next node
- `ussd_menu_action` - optional backend action before or after rendering

### Core fields

- flow: `code`, `name`, `status`, `version`
- node: `node_key`, `title`, `prompt_template`, `is_start`, `is_terminal`, `close_session`
- option: `display_order`, `input_value`, `label_template`, `next_node_key`
- transition: `condition_type`, `condition_expression`, `fallback_node_key`
- action: `service_name`, `endpoint_key`, `request_template`, `response_mapping`

### Migration strategy

1. create the tables in Liquibase as the first schema release
2. seed one default flow for `welcome -> pin -> home -> anchors -> offer`
3. keep templates in the database, not in code
4. version changes by inserting new rows and marking old versions inactive
5. do not update live menu rows destructively unless the change is purely operational

### Entity mapping approach

- JPA entities should mirror the tables directly
- use enums for fixed statuses and transition types
- store templates as plain text columns
- add unique constraints on `(flow_code, version)` and `(flow_code, node_key)`
- add indexes on `status`, `is_start`, and foreign keys used during journey resolution

### Runtime lookup flow

1. receive `sessionId` and user input
2. load session context
3. find the current node
4. resolve matching transition/option
5. load template variables from external services
6. render the next menu and persist the next node
