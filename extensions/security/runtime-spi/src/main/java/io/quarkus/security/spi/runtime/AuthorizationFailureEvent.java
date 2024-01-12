package io.quarkus.security.spi.runtime;

import java.util.Map;

import io.quarkus.security.identity.SecurityIdentity;

/**
 * Security event that should be fired when the {@link SecurityIdentity} failed security constrain,
 * such as {@link SecurityCheck} or HTTP Security policy.
 */
public final class AuthorizationFailureEvent extends AbstractSecurityEvent {
    public static final String AUTHORIZATION_FAILURE_KEY = AuthorizationFailureEvent.class.getName()
            + ".FAILURE";
    public static final String AUTHORIZATION_CONTEXT_KEY = AuthorizationFailureEvent.class.getName() + ".CONTEXT";

    public AuthorizationFailureEvent(SecurityIdentity securityIdentity, Throwable authorizationFailure,
            String authorizationContext) {
        super(securityIdentity, withProperties(authorizationFailure, authorizationContext, null));
    }

    public AuthorizationFailureEvent(SecurityIdentity securityIdentity, Throwable authorizationFailure,
            String authorizationContext, Map<String, Object> eventProperties) {
        super(securityIdentity, withProperties(authorizationFailure, authorizationContext, eventProperties));
    }

    public Throwable getAuthorizationFailure() {
        return (Throwable) eventProperties.get(AUTHORIZATION_FAILURE_KEY);
    }

    public String getAuthorizationContext() {
        return (String) eventProperties.get(AUTHORIZATION_CONTEXT_KEY);
    }

    private static Map<String, Object> withProperties(Throwable authorizationFailure, String authorizationContext,
            Map<String, Object> additionalProperties) {
        var result = withProperties(AUTHORIZATION_FAILURE_KEY, authorizationFailure, additionalProperties);
        if (authorizationContext != null) {
            result.put(AUTHORIZATION_CONTEXT_KEY, authorizationContext);
        }
        return result;
    }
}
