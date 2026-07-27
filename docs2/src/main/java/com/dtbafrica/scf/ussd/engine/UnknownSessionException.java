package com.dtbafrica.scf.ussd.engine;

/** A response arrived for a session we have no state for — expired or never started. */
public class UnknownSessionException extends RuntimeException {
    public UnknownSessionException(String sessionId) {
        super("No state for session " + sessionId);
    }
}
