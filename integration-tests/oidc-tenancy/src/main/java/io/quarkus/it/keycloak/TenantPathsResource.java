package io.quarkus.it.keycloak;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Authenticated
@Path("api/tenant-paths")
public class TenantPathsResource {

    @Inject
    RoutingContext routingContext;

    @PermitAll
    void observe(@Observes Router router) {
        router.route().order(0).handler(rc -> {
            if (rc.request().path().equals("/api/tenant-paths///public-key//match")) {
                rc.end("public-key");
            } else {
                rc.next();
            }
        });
    }

    @GET
    @Path("tenant-b/default")
    public String defaultTenant() {
        return "default";
    }

    @GET
    @Path("tenant-b/default-b")
    public String tenantB2() {
        return "tenant-b";
    }

    @GET
    @Path("tenant-b/public-key")
    public String tenantB3() {
        return "tenant-b";
    }

    @GET
    @Path("tenant-c/public-key")
    public String tenantPublicKey() {
        return "public-key";
    }

    @GET
    @Path("public-key/match")
    public String tenantPublicKey2() {
        return "public-key";
    }

    @GET
    @Path("public-key-c/match")
    public String defaultTenant2() {
        return "public-key";
    }

    @GET
    @Path("tenant-b")
    public String tenantB() {
        return "tenant-b";
    }

    @GET
    @Path("tenant-by-issuer")
    public String tenantByIssuer() {
        return routingContext.get("static.tenant.id");
    }
}
