package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.oidc.SecurityEvent;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class SecurityEventListener {

    public void event(@Observes SecurityEvent event) {
        String tenantId = event.getSecurityIdentity().getAttribute("tenant-id");
        boolean blockingApiAvailable = event.getSecurityIdentity()
                .getAttribute(AuthenticationRequestContext.class.getName()) != null;

        RoutingContext vertxContext = event.getSecurityIdentity()
                .getAttribute(RoutingContext.class.getName());
        vertxContext.put("listener-message",
                String.format("event:%s,tenantId:%s,blockingApi:%b", event.getEventType().name(), tenantId,
                        blockingApiAvailable));
    }

}
