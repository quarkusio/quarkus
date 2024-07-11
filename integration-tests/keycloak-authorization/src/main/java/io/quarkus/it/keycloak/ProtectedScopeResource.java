package io.quarkus.it.keycloak;

import static io.quarkus.security.PermissionsAllowed.PERMISSION_TO_ACTION_SEPARATOR;

import java.security.BasicPermission;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.keycloak.representations.idm.authorization.Permission;

import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

@Path("/api/permission/scopes")
public class ProtectedScopeResource {

    public static final String REQUIRED_PERMISSION = "Scope Permission Resource";
    public static final String REQUIRED_ACTION = "read";

    @Inject
    SecurityIdentity identity;

    @GET
    @Path("/standard-way")
    public Uni<List<Permission>> standardWay() {
        return Uni.createFrom().item(identity.<List<Permission>> getAttribute("permissions"));
    }

    @GET
    @Path("/standard-way-denied")
    public Uni<List<Permission>> standardWayDenied() {
        return Uni.createFrom().item(identity.<List<Permission>> getAttribute("permissions"));
    }

    @GET
    @Path("/dynamic-way")
    public Uni<List<Permission>> dynamicWay() {
        return Uni.createFrom().item(identity.<List<Permission>> getAttribute("permissions"));
    }

    @GET
    @Path("/dynamic-way-denied")
    public Uni<List<Permission>> dynamicWayDenied() {
        return Uni.createFrom().item(identity.<List<Permission>> getAttribute("permissions"));
    }

    @GET
    @Path("/programmatic-way")
    public Uni<List<Permission>> programmaticWay() {
        var requiredPermission = new BasicPermission(REQUIRED_PERMISSION) {
            @Override
            public String getActions() {
                return REQUIRED_ACTION;
            }
        };
        return identity.checkPermission(requiredPermission).onItem()
                .transform(granted -> {
                    if (granted) {
                        return identity.getAttribute("permissions");
                    }
                    throw new ForbiddenException();
                });
    }

    @GET
    @Path("/programmatic-way-denied")
    public Uni<List<Permission>> programmaticWayDenied() {
        var requiredPermission = new BasicPermission(REQUIRED_PERMISSION) {
            @Override
            public String getActions() {
                return "write";
            }
        };
        return identity.checkPermission(requiredPermission).onItem()
                .transform(granted -> {
                    if (granted) {
                        return identity.getAttribute("permissions");
                    }
                    throw new ForbiddenException();
                });
    }

    @PermissionsAllowed(REQUIRED_PERMISSION + PERMISSION_TO_ACTION_SEPARATOR + REQUIRED_ACTION)
    @GET
    @Path("/annotation-way")
    public Uni<List<Permission>> annotationWay() {
        return Uni.createFrom().item(identity.<List<Permission>> getAttribute("permissions"));
    }

    @PermissionsAllowed(REQUIRED_PERMISSION + PERMISSION_TO_ACTION_SEPARATOR + "write")
    @GET
    @Path("/annotation-way-denied")
    public Uni<List<Permission>> annotationWayDenied() {
        return Uni.createFrom().item(identity.<List<Permission>> getAttribute("permissions"));
    }
}
