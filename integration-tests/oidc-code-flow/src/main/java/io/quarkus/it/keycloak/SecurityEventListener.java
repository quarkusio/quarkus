package io.quarkus.it.keycloak;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.SecurityEvent;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class SecurityEventListener {

    public void event(@Observes SecurityEvent event) {
        String tenantId = event.getSecurityIdentity().getAttribute("tenant-id");
        boolean blockingApiAvailable = event.getSecurityIdentity()
                .getAttribute(AuthenticationRequestContext.class.getName()) != null;

        RoutingContext vertxContext = event.getSecurityIdentity().getCredential(IdTokenCredential.class).getRoutingContext();
        vertxContext.put("listener-message",
                String.format("event:%s,tenantId:%s,blockingApi:%b", event.getEventType().name(), tenantId,
                        blockingApiAvailable));
    }

}
