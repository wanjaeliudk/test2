package com.dtbafrica.scf.ussd.menu;

/**
 * One screen.
 *
 * <p>The split between {@link #render} and {@link #handle} is the core of the design.
 * A USSD screen is shown in one HTTP request and its reply arrives in a different one,
 * so anything needed to interpret the reply must already be in session state. That makes
 * a "load data for rendering" hook pointless, and lets render stay pure:
 *
 * <ul>
 *   <li>{@code render} reads session state and returns text. No I/O, no mutation.
 *   <li>{@code handle} performs the remote calls, mutates state, returns a transition.
 * </ul>
 *
 * <p>Because render is pure, the screen-budget test needs no mocks and no running
 * services, and every outbound call sits in one kind of place for timeout enforcement.
 */
public interface MenuNode {

    NodeId id();

    /** Pure. Reads session state only. Must fit the USSD screen budget. */
    String render(UssdContext ctx);

    /** Terminal screens close the session and are never sent input. */
    default boolean terminal() {
        return false;
    }

    /**
     * Standard navigation. Overridden where a key means something else — the
     * no-DTB-account screen offers "0. Exit" rather than "0. Back".
     */
    default Transition next(String input, UssdContext ctx) {
        return switch (input) {
            case "0"  -> new Transition.Back();
            case "00" -> new Transition.Root();
            default   -> handle(input, ctx);
        };
    }

    /** Does the I/O. Default treats unrecognised input as a redraw. */
    default Transition handle(String input, UssdContext ctx) {
        return new Transition.Replace(id());
    }
}
