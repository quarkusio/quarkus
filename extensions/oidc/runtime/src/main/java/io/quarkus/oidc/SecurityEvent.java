package io.quarkus.oidc;

import java.util.Map;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AbstractSecurityEvent;

/**
 * Security event.
 *
 */
public class SecurityEvent extends AbstractSecurityEvent {
    public static final String SESSION_TOKENS_PROPERTY = "session-tokens";
    public static final String AUTH_SERVER_URL = "auth-server-url";

    public enum Type {
        /**
         * OIDC connection event which is reported when an attempt to connect to the OIDC server has failed.
         */
        OIDC_SERVER_NOT_AVAILABLE,
        /**
         * OIDC connection event which is reported when a connection to the OIDC server has been recovered.
         */
        OIDC_SERVER_AVAILABLE,
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
         * OIDC Logout event is reported when the current user has started an RP-initiated OIDC logout flow but the session has
         * already expired.
         */
        OIDC_LOGOUT_RP_INITIATED_SESSION_EXPIRED,

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

    public SecurityEvent(Type eventType, SecurityIdentity securityIdentity) {
        super(securityIdentity, null);
        this.eventType = eventType;
    }

    public SecurityEvent(Type eventType, Map<String, Object> eventProperties) {
        super(null, eventProperties);
        this.eventType = eventType;
    }

    public Type getEventType() {
        return eventType;
    }

}
