package io.quarkus.security.spi.runtime;

import java.util.Map;

import io.quarkus.security.identity.SecurityIdentity;

/**
 * Security event fired when request authentication succeeded.
 * This event is never fired for anonymous {@link SecurityIdentity}.
 */
public class AuthenticationSuccessEvent extends AbstractSecurityEvent {

    public AuthenticationSuccessEvent(SecurityIdentity securityIdentity, Map<String, Object> eventProperties) {
        super(securityIdentity, eventProperties);
    }
}
