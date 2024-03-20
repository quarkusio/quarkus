package io.quarkus.vertx.http.runtime.security;

import java.util.Map;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AbstractSecurityEvent;

public final class FormAuthenticationEvent extends AbstractSecurityEvent {

    public static final String FORM_CONTEXT = "io.quarkus.vertx.http.runtime.security.FormAuthenticationEvent#CONTEXT";

    public enum FormEventType {
        /**
         * Event fired when a user was successfully authenticated with a call to the Form mechanism POST location.
         */
        FORM_LOGIN
    }

    private FormAuthenticationEvent(SecurityIdentity securityIdentity, Map<String, Object> eventProperties) {
        super(securityIdentity, eventProperties);
    }

    static FormAuthenticationEvent createLoginEvent(SecurityIdentity identity) {
        return new FormAuthenticationEvent(identity, Map.of(FORM_CONTEXT, FormEventType.FORM_LOGIN.toString()));
    }
}
