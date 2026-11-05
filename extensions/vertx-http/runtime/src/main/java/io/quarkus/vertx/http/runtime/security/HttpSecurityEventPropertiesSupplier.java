package io.quarkus.vertx.http.runtime.security;

import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;

public record HttpSecurityEventPropertiesSupplier() implements Supplier<Map<String, Object>> {
    @Override
    public Map<String, Object> get() {
        var container = Arc.requireContainer();
        if (container.requestContext().isActive()) {

            // if present, add RoutingContext from CDI request to the SecurityEvents produced in Security extension
            // it's done this way as Security extension is not Vert.x based, but users find RoutingContext useful
            var event = container.instance(CurrentVertxRequest.class).get().getCurrent();
            if (event != null) {

                if (event.user() instanceof QuarkusHttpUser user) {
                    return Map.of(RoutingContext.class.getName(), event, SecurityIdentity.class.getName(),
                            user.getSecurityIdentity());
                }

                return Map.of(RoutingContext.class.getName(), event);
            }
        }
        return Map.of();
    }
}
