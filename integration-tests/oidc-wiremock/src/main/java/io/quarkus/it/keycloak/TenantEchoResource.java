package io.quarkus.it.keycloak;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.security.Authenticated;
import io.vertx.ext.web.RoutingContext;

@HrTenant
@Authenticated
@Path("/api/tenant-echo")
public class TenantEchoResource {

    @Inject
    RoutingContext routingContext;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Map<String, String> getTenant() {
        return Stream.of(
                "static.tenant.id",
                OidcUtils.TENANT_ID_ATTRIBUTE)
                .collect(Collectors.toMap(Function.identity(), key -> "" + routingContext.get(key)));
    }
}
