# Menu engine design — Java / Spring Boot

Design sketch, not an implementation. Java 21, Spring Boot 3.x, `RestClient`.

## The principle that drives the whole design

**`render` is pure. All I/O happens in `handle`.**

This falls out of how USSD works. The gateway shows a screen in one HTTP request
(`start`/`response`) and delivers the reply in a *different* request. So anything a
screen needs in order to interpret the user's answer — the anchor list that numbers the
options, the credit limit that bounds the amount — has to survive between two requests
anyway. It must be in session state regardless.

That means a `load()` hook returning transient per-render data would be pointless.
Instead:

| Method | Does | Never does |
|---|---|---|
| `render(ctx)` | reads session state, returns the screen text | no I/O, no mutation |
| `handle(input, ctx)` | calls Profile/Trade finance, writes session state, returns a transition | — |

Two payoffs. Screens become trivially testable — the 182-character test needs no mocks
and no running services, because rendering is a pure function of state. And every remote
call sits in one kind of place, so the timeout budget from
`PROJECT_UNDERSTANDING.md` §9 has exactly one enforcement point.

## Core types

Node ids are an enum, which is what buys the compile-time safety argued for in
§12 — you cannot reference a node that does not exist, and `switch` over transitions
is exhaustive.

```java
public enum NodeId {
    // onboarding
    LANGUAGE, NOT_RECOGNISED, NO_DTB_ACCOUNT, SUPPORT, OTP_SENT,
    OTP_ENTRY, OTP_WRONG, OTP_EXPIRED,
    PIN_SET, PIN_SET_INVALID, PIN_CONFIRM, PIN_MISMATCH,
    // returning user
    PIN_LOGIN, PIN_LOGIN_WRONG, PIN_LOCKED,
    // loan
    ANCHOR_SELECT, QUALIFY, LOAN_AMOUNT, LOAN_AMOUNT_INVALID,
    LOAN_AUTHORISE_PIN, LOAN_AUTHORISE_WRONG_PIN, LOAN_SUBMITTED,
    // shared
    SERVICE_UNAVAILABLE
}
```

```java
public sealed interface Transition {
    /** Go to node, pushing the current one onto the back-stack. */
    record Goto(NodeId node) implements Transition {}
    /** Go to node without a back-stack entry — for re-rendering in place. */
    record Replace(NodeId node) implements Transition {}
    /** Pop the back-stack. */
    record Back() implements Transition {}
    /** Clear the stack, return to the journey's root. */
    record Root() implements Transition {}
}
```

```java
public interface MenuNode {

    NodeId id();

    /** Pure. Reads session state only. Must fit the USSD screen budget. */
    String render(UssdContext ctx);

    /** Terminal screens set shouldClose=true and are never sent input. */
    default boolean terminal() { return false; }

    /**
     * Standard navigation, overridable. Note the no-DTB-account screen uses
     * "0. Exit" rather than "0. Back", so it overrides this.
     */
    default Transition next(String input, UssdContext ctx) {
        return switch (input) {
            case "0"  -> new Transition.Back();
            case "00" -> new Transition.Root();
            default   -> handle(input, ctx);
        };
    }

    /** Does the I/O. Mutates session state. Returns where to go. */
    default Transition handle(String input, UssdContext ctx) {
        return new Transition.Replace(id());   // unrecognised input: redraw
    }
}
```

## Registry, with a boot-time completeness check

Spring collects every `MenuNode` bean. The constructor then asserts that every enum
constant has one — so a node you declared but never implemented fails at startup, not
at 2am when a supplier reaches that screen.

```java
@Component
public class MenuRegistry {

    private final Map<NodeId, MenuNode> nodes;

    MenuRegistry(List<MenuNode> beans) {
        this.nodes = beans.stream()
            .collect(Collectors.toUnmodifiableMap(MenuNode::id, Function.identity()));

        var missing = EnumSet.allOf(NodeId.class);
        missing.removeAll(nodes.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("No MenuNode bean for: " + missing);
        }
    }

    public MenuNode get(NodeId id) {
        return Objects.requireNonNull(nodes.get(id), () -> "unknown node " + id);
    }
}
```

## Session state

Typed fields, not a `Map<String,Object>` — this gets serialised to the session store and
read back, so the shape should be explicit.

```java
public class SessionState implements Serializable {
    private String msisdn;
    private NodeId current;
    private Deque<NodeId> backStack = new ArrayDeque<>();

    // resolved once on start (§10: one profile call)
    private ProfileSnapshot profile;

    // loan flow
    private List<Anchor> anchors = List.of();
    private int anchorPage = 0;
    private Anchor selectedAnchor;
    private CreditLimit limit;
    private BigDecimal amount;

    // onboarding
    private String pendingPin;      // between PIN_SET and PIN_CONFIRM

    // idempotency for loan submission (§9 concern 3)
    private String submissionKey;
}
```

`pendingPin` is the one uncomfortable field — a PIN in the session store between entry
and confirmation. It must never be logged, and the store should be treated as sensitive.
An alternative is to have Profile own the two-step set, which would be better if its
contract supports it.

## Engine

```java
@Service
public class UssdEngine {

    public UssdResponse start(String sessionId, String msisdn) {
        var state = new SessionState(msisdn);
        // The one profile call. Decides journey 1 vs journey 2 (§10).
        var profile = profileClient.resolveByMsisdn(msisdn);
        state.setProfile(profile);
        state.setCurrent(profile.registered() ? NodeId.PIN_LOGIN : NodeId.LANGUAGE);
        return renderCurrent(sessionId, state);
    }

    public UssdResponse respond(String sessionId, String input) {
        var state = sessions.load(sessionId).orElseThrow(UnknownSessionException::new);
        var ctx   = new UssdContext(sessionId, state);
        var node  = registry.get(state.current());

        apply(node.next(input.trim(), ctx), state);
        return renderCurrent(sessionId, state);
    }

    /** Idempotent — end can arrive at any time, including on timeout (§3). */
    public void end(String sessionId, int exitCode, String reason) {
        sessions.delete(sessionId);
        audit.sessionEnded(sessionId, exitCode, reason);
    }

    private void apply(Transition t, SessionState s) {
        switch (t) {
            case Transition.Goto g -> {
                s.backStack().push(s.current());
                s.setCurrent(g.node());
            }
            case Transition.Replace r -> s.setCurrent(r.node());
            case Transition.Back b    -> s.setCurrent(
                    s.backStack().isEmpty() ? rootFor(s) : s.backStack().pop());
            case Transition.Root r    -> {
                s.backStack().clear();
                s.setCurrent(rootFor(s));
            }
        }
    }

    private UssdResponse renderCurrent(String sessionId, SessionState state) {
        var node = registry.get(state.current());
        var text = node.render(new UssdContext(sessionId, state));
        if (node.terminal()) sessions.delete(sessionId);
        else                 sessions.save(sessionId, state);
        return new UssdResponse(node.terminal(), text, 200, "");
    }
}
```

`Goto` is what gives the back behaviour §6 requires: a validation error is reached with
`Goto`, which pushes the *input* node onto the stack, so `0. Back` from the error screen
returns to the input screen that produced it. No special casing.

## Three representative nodes

### Amount entry — validation routing

```java
@Component
class LoanAmountNode implements MenuNode {

    public NodeId id() { return NodeId.LOAN_AMOUNT; }

    public String render(UssdContext ctx) {
        return Copy.LOAN_AMOUNT_PROMPT;
    }

    public Transition handle(String input, UssdContext ctx) {
        var limit  = ctx.state().limit();          // stashed when the anchor was chosen
        var parsed = Amounts.parse(input);         // strips separators (§7 gap 8)

        if (parsed.isEmpty()
                || parsed.get().compareTo(limit.min()) < 0
                || parsed.get().compareTo(limit.max()) > 0) {
            return new Transition.Goto(NodeId.LOAN_AMOUNT_INVALID);
        }
        ctx.state().setAmount(parsed.get());
        return new Transition.Goto(NodeId.LOAN_AUTHORISE_PIN);
    }
}
```

The error screen quotes the bounds, so it reads them from the same session state:

```java
@Component
class LoanAmountInvalidNode implements MenuNode {
    public NodeId id() { return NodeId.LOAN_AMOUNT_INVALID; }
    public String render(UssdContext ctx) {
        var l = ctx.state().limit();
        return Copy.LOAN_AMOUNT_INVALID.formatted(fmt(l.min()), fmt(l.max()));
    }
}
```

### Anchor select — pagination

Closes §7 gap 5. `Replace` is used for paging so the back-stack does not fill with page
views.

```java
@Component
class AnchorSelectNode implements MenuNode {

    private static final int PAGE_SIZE = 3;   // tuned against the 182-char budget

    public NodeId id() { return NodeId.ANCHOR_SELECT; }

    public String render(UssdContext ctx) {
        var page = Pager.slice(ctx.state().anchors(), ctx.state().anchorPage(), PAGE_SIZE);
        var sb = new StringBuilder(Copy.ANCHOR_PROMPT.formatted(
                ctx.state().profile().firstName()));
        for (int i = 0; i < page.items().size(); i++) {
            sb.append("\n%d. %s".formatted(i + 1, page.items().get(i).name()));
        }
        if (page.hasNext()) sb.append(Copy.NEXT_OPTION);   // "\n99. Next"
        return sb.toString();
    }

    public Transition handle(String input, UssdContext ctx) {
        if ("99".equals(input)) {
            ctx.state().nextAnchorPage();
            return new Transition.Replace(id());
        }
        var page = Pager.slice(ctx.state().anchors(), ctx.state().anchorPage(), PAGE_SIZE);
        return Selection.index(input, page.items().size())
            .map(i -> {
                var anchor = page.items().get(i);
                ctx.state().setSelectedAnchor(anchor);
                // I/O belongs here, not in render
                ctx.state().setLimit(tradeFinance.limitFor(
                        ctx.state().profile().supplierId(), anchor.id()));
                return (Transition) new Transition.Goto(NodeId.QUALIFY);
            })
            .orElse(new Transition.Replace(id()));
    }
}
```

### Loan authorisation — the risky one

```java
@Component
class LoanAuthorisePinNode implements MenuNode {

    public NodeId id() { return NodeId.LOAN_AUTHORISE_PIN; }

    public String render(UssdContext ctx) { return Copy.AUTHORISE_PIN_PROMPT; }

    public Transition handle(String input, UssdContext ctx) {
        var s = ctx.state();

        return switch (profileClient.verifyPin(s.msisdn(), input)) {
            case WRONG  -> new Transition.Goto(NodeId.LOAN_AUTHORISE_WRONG_PIN);
            case LOCKED -> new Transition.Goto(NodeId.PIN_LOCKED);   // copy TBD, §9
            case OK     -> {
                // Stable key so a redial cannot create a second loan (§9 concern 3).
                tradeFinance.submit(new LoanApplication(
                        s.profile().supplierId(),
                        s.selectedAnchor().id(),
                        s.amount()),
                        s.submissionKey());
                yield new Transition.Goto(NodeId.LOAN_SUBMITTED);
            }
        };
    }
}
```

`submissionKey` is generated once when the amount is accepted and kept in session state,
so a retry of the same intent carries the same key.

## Copy

One class of constants, so the deferred i18n retrofit (§10) is a lookup swap rather than
an edit of every node.

```java
public final class Copy {
    public static final String LOAN_AMOUNT_PROMPT =
        "How much do you want borrow?\n0. Back";
    public static final String LOAN_AMOUNT_INVALID =
        "Please try again. The amount must be between KES %s and KES %s.\n0. Back";
    public static final String ANCHOR_PROMPT =
        "Hi %s, welcome to DTB Supply Chain Finance. Please select an anchor.";
    public static final String NEXT_OPTION = "\n99. Next";
}
```

## Outbound clients

The Public auth service is a cross-cutting client-security concern, so it belongs in a
`ClientHttpRequestInterceptor` rather than in any node — every outbound call gets
credentials without a single node knowing about it.

```java
@Configuration
class ClientConfig {

    @Bean
    RestClient profileClient(RestClient.Builder builder,
                             ProfileProperties props,
                             PublicAuthInterceptor auth) {
        var factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(props.readTimeout());       // well inside the USSD budget
        return builder
            .baseUrl(props.baseUrl())
            .requestFactory(factory)
            .requestInterceptor(auth)
            .build();
    }
}
```

The interceptor must cache and refresh its token off the hot path — never fetch one per
request, or every menu transition costs two round trips (§9 concern 2).

## Tests the design makes cheap

Because `render` is pure, the screen-budget test needs no mocks and no services:

```java
class ScreenBudgetTest {

    static final int USSD_LIMIT = 182;

    @ParameterizedTest
    @EnumSource(NodeId.class)
    void screenFitsBudget(NodeId id) {
        var text = registry.get(id).render(WorstCase.context());
        assertThat(text.length())
            .as("%s renders %d chars", id, text.length())
            .isLessThanOrEqualTo(USSD_LIMIT);
    }

    @ParameterizedTest
    @EnumSource(NodeId.class)
    void screenIsGsm7(NodeId id) {
        assertThat(Gsm7.unsupportedChars(registry.get(id).render(WorstCase.context())))
            .as("non-GSM-7 characters inflate the encoded length")
            .isEmpty();
    }
}
```

`WorstCase.context()` matters: longest plausible name, maximum amount, longest anchor
names. Rendering with `"Kamau"` and passing tells you nothing about a supplier called
Wanjiku-Kamau. The GSM-7 check catches curly quotes and dashes pasted from the design
copy, which encode outside the 7-bit alphabet and silently shrink the real budget.

## Open items this design does not resolve

- `PIN_LOCKED` and `SERVICE_UNAVAILABLE` have no approved copy (§9).
- `rootFor(state)` needs a definition — there is still no main menu in the source
  material (§7 gap 3).
- Whether Profile can own the two-step PIN set, removing `pendingPin` from our store.
- `PAGE_SIZE = 3` is a guess until real anchor name lengths are known.
