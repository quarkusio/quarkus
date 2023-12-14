package io.quarkus.resteasy.reactive.server.runtime.security;

import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHORIZATION_FAILURE;
import static io.quarkus.security.spi.runtime.SecurityEventHelper.AUTHORIZATION_SUCCESS;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;
import io.quarkus.security.spi.runtime.SecurityEventHelper;

@Singleton
public class SecurityEventContext {

    private final SecurityEventHelper<AuthorizationSuccessEvent, AuthorizationFailureEvent> helper;

    SecurityEventContext(Event<AuthorizationFailureEvent> authorizationFailureEvent,
            @ConfigProperty(name = "quarkus.security.events.enabled") boolean securityEventsEnabled,
            Event<AuthorizationSuccessEvent> authorizationSuccessEvent, BeanManager beanManager) {
        this.helper = new SecurityEventHelper<>(authorizationSuccessEvent, authorizationFailureEvent, AUTHORIZATION_SUCCESS,
                AUTHORIZATION_FAILURE, beanManager, securityEventsEnabled);
    }

    SecurityEventHelper<AuthorizationSuccessEvent, AuthorizationFailureEvent> getHelper() {
        return helper;
    }
}
