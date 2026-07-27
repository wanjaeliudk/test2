package com.dtbafrica.scf.ussd.menu;

/** Where a user's input takes the session. Sealed, so the engine's switch is exhaustive. */
public sealed interface Transition {

    /**
     * Move to {@code node}, pushing the current one onto the back-stack. This is what
     * gives validation errors the right back behaviour: the error screen is reached with
     * Goto, so "0. Back" returns to the input screen that produced it.
     */
    record Goto(NodeId node) implements Transition {}

    /** Move without a back-stack entry — redraws and pagination. */
    record Replace(NodeId node) implements Transition {}

    /** Pop the back-stack. */
    record Back() implements Transition {}

    /** Clear the stack and return to the journey root. */
    record Root() implements Transition {}
}
