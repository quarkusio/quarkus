package io.quarkus.it.keycloak;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
