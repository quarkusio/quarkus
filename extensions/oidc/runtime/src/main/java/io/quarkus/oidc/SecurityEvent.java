package io.quarkus.oidc;

import java.util.Map;

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
        OIDC_LOGOUT_RP_INITIATED,

        /**
         * OIDC BackChannel Logout initiated event is reported when the BackChannel logout request to logout the current user
         * has been received.
         */
        OIDC_BACKCHANNEL_LOGOUT_INITIATED,

        /**
         * OIDC BackChannel Logout completed event is reported when the current user's session has been removed due to a pending
         * OIDC
         * BackChannel logout request.
         */
        OIDC_BACKCHANNEL_LOGOUT_COMPLETED,
        /**
         * OIDC FrontChannel Logout event is reported when the current user's session has been removed due to an OIDC
         * FrontChannel logout request.
         */
        OIDC_FRONTCHANNEL_LOGOUT_COMPLETED
    }

    private final Type eventType;
    private final SecurityIdentity securityIdentity;
    private final Map<String, Object> eventProperties;

    public SecurityEvent(Type eventType, SecurityIdentity securityIdentity) {
        this.eventType = eventType;
        this.securityIdentity = securityIdentity;
        this.eventProperties = Map.of();
    }

    public SecurityEvent(Type eventType, Map<String, Object> eventProperties) {
        this.eventType = eventType;
        this.securityIdentity = null;
        this.eventProperties = eventProperties;
    }

    public Type getEventType() {
        return eventType;
    }

    public SecurityIdentity getSecurityIdentity() {
        return securityIdentity;
    }

    public Map<String, Object> getEventProperties() {
        return eventProperties;
    }
}
