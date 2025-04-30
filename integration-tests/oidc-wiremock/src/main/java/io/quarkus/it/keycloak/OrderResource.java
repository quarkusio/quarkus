package io.quarkus.it.keycloak;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.core.eventbus.EventBus;

@Path("order/bearer")
public class OrderResource {

    @Inject
    EventBus eventBus;

    @Inject
    SecurityIdentity identity;

    @POST
    public void order(String product, @HeaderParam(AUTHORIZATION) String bearer) {
        if (!"alice".equals(identity.getPrincipal().getName())) {
            // point here is to make sure that identity is resolved and later, when the event is consumed
            // this identity won't be available as it will be brand-new request context
            throw new UnauthorizedException("Only Alice is allowed to access this endpoint");
        }
        String rawToken = bearer.substring("Bearer ".length());
        eventBus.publish("product-order", new Product(product, 1, rawToken));
    }

    @GET
    public String acquiredIdentities() {
        return String.join(" ", OrderService.IDENTITY_REPOSITORY);
    }
}
