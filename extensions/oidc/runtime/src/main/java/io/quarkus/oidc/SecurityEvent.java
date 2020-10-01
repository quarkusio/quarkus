package io.quarkus.oidc;

import io.quarkus.security.identity.SecurityIdentity;

/**
 * Security event.
 *
 */
public class SecurityEvent {
    public enum Type {
        /**
         * OIDC Login event which is reported after the first user authentication but also when the user's session
         * has expired and the user has re-authenticated at the OIDC provider site.
         */
        OIDC_LOGIN,
        /**
         * OIDC Session refreshed event is reported if it has been detected that an ID token will expire shortly and the session
         * has been successfully auto-refreshed without the user having to re-authenticate again at the OIDC site.
         */
        OIDC_SESSION_REFRESHED,
        /**
         * OIDC Session expired and refreshed event is reported if a session has expired but been successfully refreshed
         * without the user having to re-authenticate again at the OIDC site.
         */
        OIDC_SESSION_EXPIRED_AND_REFRESHED,
        /**
         * OIDC Logout event is reported when the current user has started an RP-initiated OIDC logout flow.
         */
        OIDC_LOGOUT_RP_INITIATED
    }

    private final Type eventType;
    private final SecurityIdentity securityIdentity;

    public SecurityEvent(Type eventType, SecurityIdentity securityIdentity) {
        this.eventType = eventType;
        this.securityIdentity = securityIdentity;
    }

    public Type getEventType() {
        return eventType;
    }

    public SecurityIdentity getSecurityIdentity() {
        return securityIdentity;
    }
}
