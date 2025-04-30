package io.quarkus.it.keycloak;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import io.quarkus.security.spi.runtime.AuthenticationFailureEvent;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class AuthenticationFailureEventListener {

    private final Set<String> failedPaths = ConcurrentHashMap.newKeySet();

    void observe(@Observes AuthenticationFailureEvent event) {
        RoutingContext ctx = (RoutingContext) event.getEventProperties().get(RoutingContext.class.getName());
        if (ctx.request().headers().contains("keep-event")) {
            failedPaths.add(ctx.request().path());
        }
    }

    Set<String> getFailedPaths() {
        return failedPaths;
    }
}
