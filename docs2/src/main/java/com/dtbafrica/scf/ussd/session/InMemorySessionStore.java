package com.dtbafrica.scf.ussd.session;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Development placeholder. Not production-safe: no TTL and no sharing across instances,
 * so sessions are lost on restart and break behind a load balancer.
 */
@Component
public class InMemorySessionStore implements SessionStore {

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    @Override
    public Optional<SessionState> load(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public void save(String sessionId, SessionState state) {
        sessions.put(sessionId, state);
    }

    @Override
    public void delete(String sessionId) {
        sessions.remove(sessionId);
    }
}
