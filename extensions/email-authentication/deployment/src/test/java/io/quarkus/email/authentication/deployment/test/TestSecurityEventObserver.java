package io.quarkus.email.authentication.deployment.test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.email.authentication.EmailAuthenticationEvent;
import io.quarkus.security.spi.runtime.AuthenticationFailureEvent;

@ApplicationScoped
public class TestSecurityEventObserver {

    private final List<EmailAuthenticationEvent> emailAuthenticationEvents = new CopyOnWriteArrayList<>();
    private final List<AuthenticationFailureEvent> authFailedEvents = new CopyOnWriteArrayList<>();

    void observeEvent(@Observes EmailAuthenticationEvent event) {
        emailAuthenticationEvents.add(event);
    }

    void observeEvent(@Observes AuthenticationFailureEvent event) {
        authFailedEvents.add(event);
    }

    List<EmailAuthenticationEvent> getEmailAuthenticationEvents() {
        return List.copyOf(emailAuthenticationEvents);
    }

    public List<AuthenticationFailureEvent> getAuthFailedEvents() {
        return List.copyOf(authFailedEvents);
    }

    void clear() {
        emailAuthenticationEvents.clear();
        authFailedEvents.clear();
    }

}
