package com.dtbafrica.scf.ussd.menu;

import com.dtbafrica.scf.ussd.session.SessionState;

/** What a node is given: the session id and the mutable state for this session. */
public record UssdContext(String sessionId, SessionState state) {

    public String msisdn() {
        return state.getMsisdn();
    }
}
