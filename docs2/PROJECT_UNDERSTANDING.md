# DTB Supply Chain Finance — USSD Service

Understanding derived from the three documents currently in scope. Nothing here is
implementation; it is a readback of the specs so we can agree on the target before
writing code.

## 1. Source documents

| Document | Location | What it defines |
|---|---|---|
| Infobip USSD Gateway REST/JSON API v1.4 | `Infobip-USSD-Gateway_REST-JSON-API_v1.4.pdf` | The transport contract — how the gateway calls us |
| Registration and initial loan application journey | `registration-and-initial-loan-application-journey.jpeg` | First-time user flow |
| Subsequent journey after registration | `subsequest-journey-after-registration.jpeg` | Returning user flow |

## 2. What the product is

An agri **supply chain finance** product for Diamond Trust Bank, delivered over USSD
so it works on feature phones.

The actors:

- **Supplier** — the farmer, the USSD user. Identified by MSISDN.
- **Anchor** — the offtaker/buyer the supplier delivers to (the diagram uses
  *Kabianga*, a tea factory). The anchor relationship is what makes the supplier
  creditworthy.
- **DTB** — the lender. Requires the supplier to hold a DTB account.

The supplier borrows against their delivery relationship with an anchor. Their limit
is derived per-anchor (`KES 45,789 from Kabianga` in the mock), so the limit is a
function of *(supplier, anchor)*, not of the supplier alone.

Note a **branding inconsistency in the source material**: registration screens say
"DTB Agriloans", the returning-user screen says "DTB Supply Chain Finance". One needs
to win.

## 3. Transport contract (Infobip)

Three endpoints, keyed by a gateway-issued `sessionId` in the path:

| Method | Path | Called when |
|---|---|---|
| `POST` | `{baseUrl}/session/{sessionId}/start` | Session opens — return the first menu |
| `PUT` | `{baseUrl}/session/{sessionId}/response` | User replied — return the next menu |
| `PUT` | `{baseUrl}/session/{sessionId}/end` | Session closed or aborted — clean up |

**Request fields** (`start` and `response`): `msisdn` (mandatory), `imsi`,
`shortCode`, `optional`, `ussdNodeId`, `text`, `networkName`, `countryName`.
On `start`, `text` is auxiliary trigger data. On `response`, `text` is the user's reply.

**Response fields** (`start` and `response`) — all four mandatory:

```json
{
  "shouldClose": false,
  "ussdMenu": "line one\nline two",
  "responseExitCode": 200,
  "responseMessage": ""
}
```

`shouldClose: true` marks a terminal notification. `\n` is the line break.

**`end` request** carries `reason` and `exitCode`; we reply with only
`responseExitCode` + `responseMessage`.

**Exit codes.** Ours (`responseExitCode`): `200` ok, `400` JSON parse error,
`500` internal error. Theirs, on `end` (`exitCode`): `200` normal, `500` aborted by
network, `510` aborted by TPA, `520` aborted by user, `600` timeout.

**Constraints that follow from this design:**

- `baseUrl` is configured in the Infobip application editor and **must not have a
  trailing slash**.
- Auth is optional HTTP **Basic**, configured on the Infobip side; they add the
  `Authorization` header to calls into us. So the app needs a Basic-auth guard on
  these three routes, credentials supplied by config.
- Session state is entirely our problem — the gateway gives us a `sessionId` and
  nothing else. We hold the menu position.
- `end` can arrive at any time, including on timeout, and must be idempotent.

## 4. Registration and first loan (journey 1)

### Session A — identification

Entry screen:

```
Hello and welcome to DTB Agriloans.
1. Proceed in English
2. Endelea kwa Kiswahili
```

After language selection the flow branches on an **MSISDN lookup**, three ways:

| Condition | Screen | Closes? |
|---|---|---|
| Number recognised (known supplier, has DTB account) | `An OTP has been sent to you. Exit now, check your messages, and dial back as soon as possible.` | yes |
| Number recognised, **no DTB account** | `Your number is registered but you need a DTB account to access a loan with DTB Agriloans.` + `Please contact customer support or visit the nearest branch.` / `1. Contact customer support` / `0. Exit` | no |
| Number **not** recognised | `You do not qualify for DTB Agriloans.` / `1. Contact customer support` / `0. Back` | no |

Both failure branches route to the support screen:

```
We are here to support you around the clock, 24/7.
Email us on : contactcentre@dtbafrica.com
Call us on :
+254 202849888
+254 719031888
+254 732121888

0. Back
00. Main menu
```

### Session B — the user dials back

The diagram explicitly marks this **"Restarts session"**. The OTP arrives by SMS,
out of band, and the USSD session has already closed. The user redials and sees the
language screen again, then:

```
Please enter the OTP that was sent to you.
0. Back
```

| Input | Result |
|---|---|
| Wrong OTP | `Wrong OTP. Please try again.` / `0. Back` |
| Expired OTP | `The OTP you entered has expired.` / `1. Resend OTP.` |
| Correct OTP | proceed to PIN setup |

PIN setup:

```
Set a new PIN. Should be 4 digits.
0. Back
```

| Input | Result |
|---|---|
| Not 4 digits | `Invalid PIN, please try again. PIN should be 4 digits.` / `0. Back` |
| Valid | `Confirm your new PIN.` / `0. Back` |
| Confirmation mismatch | `PINs don't match. Please try again.` / `0. Back` |
| Match | `You're good to go! Please select an anchor.` / `1. Kabianga` / `0. Back` |

### Loan application

```
You qualify for a loan of upto KES 45,789 from Kabianga.
1. Apply for loan
0. Back
```

→ `How much do you want borrow?` / `0. Back`

| Input | Result |
|---|---|
| Out of range | `Please try again. The amount must be between KES 100 and KES 45,789.` / `0. Back` |
| Valid | `Please enter your PIN to authorise loan application.` / `0. Back` |
| Wrong PIN | `Wrong PIN, please try again.` / `0. Back` |
| Correct PIN | `Your request has been received and is being processed.` / `00. Main Menu` |

Floor is **KES 100**, ceiling is the per-anchor limit.

## 5. Returning user (journey 2)

Shorter — no language screen, no OTP, no PIN setup. Greets by name.

```
Hi Kamau. Please enter your PIN to proceed.
1. Forgot PIN
```

→ correct PIN →

```
Hi Kamau, welcome to DTB Supply Chain Finance. Please select an anchor.
1. Kabianga
```

→ then the identical loan tail: qualify → amount → authorising PIN → received.
Same two error screens (amount range, wrong PIN).

Implication: language and PIN are persisted on the supplier at registration, and the
returning flow reads them rather than asking.

## 6. Navigation conventions

Consistent across both journeys:

- `1..n` — numbered menu options
- `0` — Back (one level up)
- `00` — Main Menu
- Plain input — free text for OTP, PIN, amount

A back-stack is required, not just a "previous state" pointer, because `0. Back` from
a validation-error screen returns to the *input* screen that produced it.

## 7. Open questions and gaps in the source material

These are genuinely unspecified — they need answers before or during build.

1. **`1. Forgot PIN` has no destination.** The arrow leads nowhere in the diagram.
   Presumably re-OTP then re-set PIN, reusing the registration tail — but it needs
   confirming, and it is a security-sensitive path.
2. **Kiswahili has no copy.** Option `2. Endelea kwa Kiswahili` exists on the entry
   screen, but every screen in both diagrams is English-only. Either a Swahili copy
   deck is missing, or i18n is deferred. This materially changes scope: it means every
   string is a keyed, translated resource rather than a literal.
3. **No main menu exists.** Two screens offer `00. Main Menu` but no main menu is ever
   drawn. For a returning user the flow jumps PIN → anchor select. Is anchor-select
   the main menu? There is no "check balance", "my loans", or "repay" anywhere — which
   is unusual for a lending product and suggests the menu is out of scope for these
   two diagrams rather than absent from the product.
4. **No retry limits.** "Wrong PIN, please try again" and "Wrong OTP, please try again"
   loop with no stated attempt cap or lockout. Both need one; PIN especially, since a
   4-digit PIN over a redialable channel is brute-forceable. Also unstated: OTP
   validity window, OTP resend rate limit.
5. **Anchor list is single-item.** The copy says "select an anchor", implying a list.
   With more than a handful, the 182-char screen limit forces pagination — needs a
   `99. Next` convention that the diagrams don't define.
6. **Character-limit overflow.** USSD caps a screen at 182 GSM-7 characters (some
   networks 160). The "no DTB account" screen measures **187 characters** and will be
   truncated as drawn. The support screen is 173 — under, but with no headroom, and
   over if any network enforces 160. Copy
   needs a pass against a hard limit, and the engine should enforce it at build time.
7. **Registration spans two USSD sessions.** This is the most important architectural
   consequence in the whole spec: OTP state and registration progress **cannot** live
   in session storage, because the session is deliberately destroyed between issuing
   the OTP and consuming it. There are two distinct lifetimes:
   - *session* state (menu position, back-stack, partial input) — seconds, dies with
     the session
   - *registration/onboarding* state (OTP hash, expiry, attempt count, PIN-set
     progress) — minutes, survives across sessions, keyed by MSISDN
8. **Amount formatting.** The mock shows `20,000` typed with a comma separator.
   Feature-phone input is digits-only in practice; parsing should strip separators and
   the prompt should probably say so.
9. **Loan application is asynchronous.** "received and is being processed" — so there
   is a downstream decisioning system. Its interface is undefined: do we call a core
   banking / LOS API, publish an event, or write a queue row? Also undefined: how the
   supplier learns the outcome (SMS, presumably).
10. **Limit source is undefined.** `KES 45,789` is oddly precise, so it is computed
    upstream (delivery history against the anchor). Where do we read it from?

## 8. Repository state

- Repo: `github.com/wanjaeliudk/test2`, branch `main`, 3 commits.
- `trade-finance-ussd-services` is committed as a **gitlink** (mode `160000`,
  commit `fb898b3`) with **no `.gitmodules` entry** — an accidental nested-repo
  commit, not a real directory. Git will not track anything placed inside it until
  the entry is removed from the index. This blocks any work in that path and should
  be cleared first.
- `.DS_Store` is committed and should be gitignored.

## 9. External service integration

The USSD app is a **presentation and orchestration layer**, not a system of record. It
owns the menu tree, the session/back-stack, and copy. Everything else lives behind:

- **Profile service** — the entire identity and credential journey: identify caller,
  OTP issue/verify, set PIN, verify PIN, forgot PIN
- **Trade finance service** — anchor list, limit, submit application
- **Public auth service** — *not* a journey participant. This is the client-to-client
  API security layer, consumed via a REST client. It is how the USSD app authenticates
  itself when calling Profile and Trade finance.

So there are **two functional dependencies** (Profile, Trade finance) and **one
cross-cutting concern** (Public auth) that wraps every outbound call.

### Capability map

Each journey step and the call it implies. Inferred from the diagrams — this is the
checklist to validate against the real contracts.

| Step | Service | Capability needed | Returns |
|---|---|---|---|
| `start` — identify caller | Profile | resolve by MSISDN | exists, name, language pref, has-DTB-account, registration status |
| OTP issue | Profile | request OTP for MSISDN (service sends the SMS) | accepted, expiry |
| OTP verify | Profile | verify code | ok / **wrong** / **expired** — must be distinguishable, they are different screens |
| Set PIN | Profile | set PIN, gated by OTP result | ok / policy violation |
| PIN verify (login, and loan authorise) | Profile | verify PIN | ok / wrong / **locked** |
| Forgot PIN | Profile | reset flow | undefined — see gap 1 |
| Anchor list | Trade finance | anchors for this supplier | id + display name per anchor |
| Qualification | Trade finance | limit for *(supplier, anchor)* | max, min (KES 100 in mock) |
| Submit application | Trade finance | create loan application | reference; async thereafter |
| *(every call above)* | Public auth | obtain/attach client credentials | token or equivalent |

### What this resolves

**Gap 7 is dissolved.** Profile owns the OTP lifecycle keyed by MSISDN, so the state
that had to survive the deliberate session teardown is *theirs*. We need only
**session state** — menu position, back-stack, partial input such as the entered
amount, and any token held mid-session. No cross-session onboarding store.

**Gap 4 moves to Profile.** It owns PIN verification, so it owns the attempt counter
and lockout. Better — but the UI must then render a **locked-out state that neither
diagram contains**. New copy needed.

**Service-to-service auth is answered:** the Public auth service provides it. What
remains is the mechanics — token lifetime, caching, refresh, and whether a token
fetch sits on the hot path (see concern 2).

### Remaining concerns

1. **Profile is now a single point of failure for the whole journey.** Identity, OTP
   and PIN all sit behind it. If Profile is down, nothing works — not even the
   "you don't qualify" branch. Worth knowing its availability target.
2. **Latency budget is the hard constraint.** A USSD session times out at the network
   (Infobip signals `exitCode 600`), typically tens of seconds, and every menu
   transition may fan out to HTTP — now potentially *two* calls, since Public auth may
   need to issue a token before the functional call. Tokens must be cached and
   refreshed off the hot path, never fetched per request. Each call needs an explicit
   timeout well inside the gateway's, with a defined degraded screen on breach.
3. **Idempotency on loan submission is mandatory.** USSD is lossy and users redial
   after timeouts. Without an idempotency key on the trade-finance create call, a
   retried or double-submitted application becomes two loans. Highest-risk integration
   point in the flow.
4. **Session token propagation.** Does Profile's PIN verify return a user-scoped token
   that the trade-finance calls then require — distinct from the client-scoped Public
   auth token? If so, session state carries both, and their TTLs interact with the USSD
   session lifetime.
5. **Failure copy is undefined throughout.** Neither diagram shows a "service
   unavailable" screen. Every call can fail, and USSD offers no retry affordance beyond
   redialing. Needs a generic fallback.
6. **Secrets handling.** Public auth credentials, plus PINs and OTPs transiting the app.
   None may be logged. Worth an explicit redaction rule in the logger config, since
   Pino-style request logging will otherwise capture request bodies containing PINs.

## 10. Decisions taken

**Registration status is owned by the Profile service** (a status field), not by auth.

Consequence: the `start` handler makes a **single profile call** that returns identity,
registration status, language preference and has-DTB-account together. That one round
trip resolves both the journey-1/journey-2 fork and the three-way branch in section 4 —
the best possible outcome for the latency budget in section 9.

**Kiswahili is deferred.** English literals for the first cut; i18n is a later retrofit.

Open follow-on, small: the entry screen still renders `2. Endelea kwa Kiswahili`. Pressing
it has to do *something*. Either drop the option until the copy deck exists, or accept it
and proceed in English. Recommend dropping it — silently ignoring a language the user
explicitly chose is worse than not offering it. This changes approved design copy, so it
is your call.

**API contracts will be supplied** for Profile, Public auth, and Trade finance.

## 11. Decisions still needed

- Where the code lives: `test2/` root, or inside `trade-finance-ussd-services/`
  (which currently needs the stale gitlink cleared first — see section 8).
- Stack.
- Session store. The separate cross-session onboarding store is no longer needed —
  see section 9.
- Retry/lockout policy ownership, and the lockout screen copy that neither diagram has.
- Idempotency key strategy for loan submission (section 9, concern 3).

## 12. Menu storage — recommendation

**Define the menu tree in typed code in the repo. Use Postgres (with migrations) for
runtime state only.**

### Why not Postgres for the menus

1. **These screens are not static data.** `You qualify for a loan of upto KES 45,789
   from Kabianga.` interpolates two service-derived values. The `start` screen branches
   three ways on profile status. Nodes carry input validators (4 digits, amount within
   range) and the anchor list needs pagination. All of that is logic, and it lives in
   code regardless of where the strings live. Putting copy in a table therefore splits
   one menu tree across two places — worse than either option alone.
2. **The 182-character limit can only be enforced pre-production if copy is in the
   repo.** A CI test can assert every screen renders within budget. This is not
   hypothetical: the "no DTB account" screen is already 187 characters (section 7,
   gap 6). In a table, that violation ships.
3. **Change control.** Customer-facing copy for a bank benefits from diff, PR, approval,
   blame and atomic rollback — which git gives free, and which ships in lockstep with
   the code that renders it. A table lets someone alter bank copy with an `UPDATE` and
   no review. In a regulated context that is a liability, not flexibility.
4. **Latency.** No query on the hot path, which matters given the budget in section 9.
5. **It does not even buy deploy-free changes.** Changing copy via a migration is still
   a deploy — so you take on the database complexity *and* keep the deploy.

### Why typed code rather than a JSON file

JSON needs a schema plus runtime validation to catch a mistyped node id or a transition
to a node that does not exist. Typed definitions catch both at compile time, and give
exhaustiveness checking over transitions. If an inspectable artifact is wanted, the
typed tree can be serialised to JSON for documentation or diagram generation — one
direction only, code stays the source of truth.

Suggested node shape:

```
{ id, render(ctx) -> string, options?, validate?(input), next(input, ctx) -> nodeId }
```

Copy extracted into a separate strings module, so the deferred i18n retrofit
(section 10) is a lookup swap rather than a rewrite of every node.

### What does belong in Postgres, with migrations

- **Session state** — menu position, back-stack, partial input, tokens. Redis with a TTL
  is the more natural fit; Postgres is fine if you would rather run one datastore.
- **Interaction/audit log** — MSISDN, session id, node, input, response, timings. Genuinely
  valuable in banking for dispute resolution and for debugging flows you cannot reproduce.
  PINs and OTPs must be redacted here (section 9, concern 6).
- **Idempotency keys** for loan submission (section 9, concern 3) — needs durable storage.
- Optionally a short-lived cache of anchor lists.

### When the database answer would be right

If non-engineers must change copy without a deploy, or menus vary per anchor/tenant at
runtime, or you want to A/B test copy. None of these are in evidence in the current
scope. Even then, the usual answer is a config service with a review workflow rather
than raw tables.
