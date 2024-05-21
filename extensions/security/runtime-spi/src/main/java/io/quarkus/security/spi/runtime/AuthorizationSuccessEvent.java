package io.quarkus.security.spi.runtime;

import java.util.Map;

import io.quarkus.security.identity.SecurityIdentity;

/**
 * Security event that should be fired when the {@link SecurityIdentity} passed security constrain,
 * such as {@link SecurityCheck} or HTTP Security policy.
 */
public final class AuthorizationSuccessEvent extends AbstractSecurityEvent {
    public static final String AUTHORIZATION_CONTEXT = AuthorizationSuccessEvent.class.getName() + ".CONTEXT";
    public static final String SECURED_METHOD_KEY = AuthorizationSuccessEvent.class.getName() + ".SECURED_METHOD";

    public AuthorizationSuccessEvent(SecurityIdentity securityIdentity, Map<String, Object> eventProperties) {
        super(securityIdentity, eventProperties);
    }

    public AuthorizationSuccessEvent(SecurityIdentity securityIdentity, String authorizationContext,
            Map<String, Object> eventProperties) {
        super(securityIdentity, withProperties(AUTHORIZATION_CONTEXT, authorizationContext, eventProperties));
    }

    public AuthorizationSuccessEvent(SecurityIdentity securityIdentity, String authorizationContext,
            Map<String, Object> eventProperties, MethodDescription securedMethod) {
        this(securityIdentity, authorizationContext, withProperties(SECURED_METHOD_KEY, toString(securedMethod),
                eventProperties));
    }
}
