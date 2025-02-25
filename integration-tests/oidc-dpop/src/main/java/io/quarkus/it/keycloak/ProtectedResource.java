package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.security.Authenticated;
import io.vertx.ext.web.RoutingContext;

@Path("/service")
@Authenticated
public class ProtectedResource {

    @Inject
    JsonWebToken principal;

    @Inject
    RoutingContext routingContext;

    @GET
    @Produces("text/plain")
    @Path("dpop-jwt")
    public String hello() {
        return "Hello, " + principal.getName() + "; "
                + "JWK thumbprint in JWT: " + isJwtTokenThumbprintAvailable() + ", "
                + "JWK thumbprint in introspection: " + isIntrospectionThumbprintAvailable();
    }

    private boolean isJwtTokenThumbprintAvailable() {
        return Boolean.TRUE.equals(routingContext.get(OidcUtils.DPOP_JWT_THUMBPRINT));
    }

    private boolean isIntrospectionThumbprintAvailable() {

        return Boolean.TRUE.equals(routingContext.get(OidcUtils.DPOP_INTROSPECTION_THUMBPRINT));
    }
}
