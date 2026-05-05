package io.quarkus.email.authentication.runtime.internal;

import static io.quarkus.email.authentication.EmailAuthenticationEvent.EmailAuthenticationEventType.AUTHENTICATION_CODE;
import static io.quarkus.email.authentication.EmailAuthenticationEvent.EmailAuthenticationEventType.EMAIL_LOGIN;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.ROUTING_CONTEXT_ATTRIBUTE;

import java.util.Map;

import io.quarkus.email.authentication.EmailAuthenticationEvent;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.AbstractSecurityEvent;
import io.vertx.ext.web.RoutingContext;

final class EmailAuthenticationEventImpl extends AbstractSecurityEvent implements EmailAuthenticationEvent {

    private final EmailAuthenticationEventType eventType;

    EmailAuthenticationEventImpl(EmailAuthenticationEventType eventType, SecurityIdentity securityIdentity,
            Map<String, Object> eventProperties) {
        super(securityIdentity, eventProperties);
        this.eventType = eventType;
    }

    @Override
    public EmailAuthenticationEventType getEventType() {
        return eventType;
    }

    static EmailAuthenticationEvent createEmptyEvent() {
        return new EmailAuthenticationEventImpl(EMAIL_LOGIN, null, null);
    }

    static EmailAuthenticationEvent createLoginEvent(SecurityIdentity identity, String code, RoutingContext event) {
        return new EmailAuthenticationEventImpl(EMAIL_LOGIN, identity,
                Map.of(AUTHENTICATION_CODE_KEY, code, ROUTING_CONTEXT_ATTRIBUTE, event));
    }

    static EmailAuthenticationEvent createAuthenticationCodeEvent(String emailAddress, char[] code, RoutingContext event) {
        return new EmailAuthenticationEventImpl(AUTHENTICATION_CODE, null, Map.of(AUTHENTICATION_CODE_KEY, code,
                EMAIL_ADDRESS_KEY, emailAddress, ROUTING_CONTEXT_ATTRIBUTE, event));
    }

    static EmailAuthenticationEvent createAuthenticationCodeEvent(Throwable failure, String emailAddress,
            RoutingContext event) {
        return new EmailAuthenticationEventImpl(AUTHENTICATION_CODE, null,
                Map.of(EMAIL_ADDRESS_KEY, emailAddress, FAILURE_KEY, failure, ROUTING_CONTEXT_ATTRIBUTE, event));
    }
}
