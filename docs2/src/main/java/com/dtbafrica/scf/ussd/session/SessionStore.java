package com.dtbafrica.scf.ussd.session;

import java.util.Optional;

/**
 * Keyed by the gateway's sessionId. Redis with a TTL is the natural production fit;
 * this port keeps that choice out of the menu code.
 */
public interface SessionStore {

    Optional<SessionState> load(String sessionId);

    void save(String sessionId, SessionState state);

    void delete(String sessionId);
}
