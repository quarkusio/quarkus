package io.quarkus.security.spi.runtime;

import java.util.Map;

/**
 * Security event fired when authentication failed.
 */
public class AuthenticationFailureEvent extends AbstractSecurityEvent {

    public static final String AUTHENTICATION_FAILURE_KEY = AuthenticationFailureEvent.class.getName() + ".FAILURE";

    public AuthenticationFailureEvent(Throwable authenticationFailure, Map<String, Object> eventProperties) {
        super(null, withProperties(AUTHENTICATION_FAILURE_KEY, authenticationFailure, eventProperties));
    }

    public Throwable getAuthenticationFailure() {
        return (Throwable) eventProperties.get(AUTHENTICATION_FAILURE_KEY);
    }
}
