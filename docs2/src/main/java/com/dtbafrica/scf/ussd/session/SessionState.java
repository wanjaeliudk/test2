package com.dtbafrica.scf.ussd.session;

import com.dtbafrica.scf.ussd.domain.Anchor;
import com.dtbafrica.scf.ussd.domain.CreditLimit;
import com.dtbafrica.scf.ussd.domain.ProfileSnapshot;
import com.dtbafrica.scf.ussd.menu.NodeId;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Everything that must survive between the request that renders a screen and the request
 * that delivers the reply. Typed fields rather than a map, because this is serialised to
 * the session store and read back.
 *
 * <p>Lifetime is the USSD session only — seconds. Cross-session onboarding state (OTP
 * issue and expiry) belongs to the Profile service, not here.
 */
public class SessionState implements Serializable {

    private final String msisdn;
    private NodeId current;
    private final Deque<NodeId> backStack = new ArrayDeque<>();

    private ProfileSnapshot profile;

    private List<Anchor> anchors = new ArrayList<>();
    private int anchorPage;
    private Anchor selectedAnchor;
    private CreditLimit limit;
    private BigDecimal amount;

    /**
     * The first PIN, held between entry and confirmation.
     *
     * <p>This is the one field carrying a secret. It must never be logged, and the store
     * holding it is sensitive. If Profile can own the two-step set-and-confirm, this field
     * disappears.
     */
    private transient String pendingPin;

    /** Generated once when an amount is accepted, so a retry cannot create a second loan. */
    private String submissionKey;

    /** Distinguishes the "You're good to go!" anchor prompt from the returning-user one. */
    private boolean justRegistered;

    public SessionState(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getMsisdn() { return msisdn; }

    public NodeId getCurrent() { return current; }
    public void setCurrent(NodeId current) { this.current = current; }

    public Deque<NodeId> backStack() { return backStack; }

    public ProfileSnapshot getProfile() { return profile; }
    public void setProfile(ProfileSnapshot profile) { this.profile = profile; }

    public List<Anchor> getAnchors() { return anchors; }
    public void setAnchors(List<Anchor> anchors) {
        this.anchors = new ArrayList<>(anchors);
        this.anchorPage = 0;
    }

    public int getAnchorPage() { return anchorPage; }
    public void nextAnchorPage() { this.anchorPage++; }

    public Anchor getSelectedAnchor() { return selectedAnchor; }
    public void setSelectedAnchor(Anchor selectedAnchor) { this.selectedAnchor = selectedAnchor; }

    public CreditLimit getLimit() { return limit; }
    public void setLimit(CreditLimit limit) { this.limit = limit; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPendingPin() { return pendingPin; }
    public void setPendingPin(String pendingPin) { this.pendingPin = pendingPin; }
    public void clearPendingPin() { this.pendingPin = null; }

    public String getSubmissionKey() { return submissionKey; }
    public void setSubmissionKey(String submissionKey) { this.submissionKey = submissionKey; }

    public boolean isJustRegistered() { return justRegistered; }
    public void setJustRegistered(boolean justRegistered) { this.justRegistered = justRegistered; }

    /** Never include pendingPin. */
    @Override
    public String toString() {
        return "SessionState{msisdn=%s, current=%s, anchorPage=%d}"
                .formatted(msisdn, current, anchorPage);
    }
}
