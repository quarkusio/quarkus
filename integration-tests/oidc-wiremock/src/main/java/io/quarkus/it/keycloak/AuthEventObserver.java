package io.quarkus.it.keycloak;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.security.spi.runtime.AuthenticationSuccessEvent;
import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;
import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;

@Dependent
@IfBuildProfile("jax-rs-http-perms-test")
public class AuthEventObserver {

    private static final List<AuthorizationSuccessEvent> authorizationSuccessEvents = new CopyOnWriteArrayList<>();
    private static final List<AuthorizationFailureEvent> authorizationFailureEvents = new CopyOnWriteArrayList<>();
    private static final List<AuthenticationSuccessEvent> authenticationSuccessEvents = new CopyOnWriteArrayList<>();

    void observe(@Observes AuthorizationSuccessEvent authZSuccess) {
        authorizationSuccessEvents.add(authZSuccess);
    }

    void observe(@Observes AuthorizationFailureEvent authZFailure) {
        authorizationFailureEvents.add(authZFailure);
    }

    void observe(@Observes AuthenticationSuccessEvent authenticationSuccessEvent) {
        authenticationSuccessEvents.add(authenticationSuccessEvent);
    }

    public static List<AuthorizationSuccessEvent> getAuthorizationSuccessEvents() {
        return List.copyOf(authorizationSuccessEvents);
    }

    public static List<AuthorizationFailureEvent> getAuthorizationFailureEvents() {
        return List.copyOf(authorizationFailureEvents);
    }

    public static List<AuthenticationSuccessEvent> getAuthenticationSuccessEvents() {
        return List.copyOf(authenticationSuccessEvents);
    }

    public static void clearEvents() {
        authorizationFailureEvents.clear();
        authorizationSuccessEvents.clear();
        authenticationSuccessEvents.clear();
    }
}
