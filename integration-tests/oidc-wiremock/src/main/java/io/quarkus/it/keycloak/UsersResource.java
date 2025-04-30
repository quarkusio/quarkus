package io.quarkus.it.keycloak;

import static io.quarkus.oidc.runtime.OidcUtils.TENANT_ID_ATTRIBUTE;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.it.keycloak.model.User;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.ext.web.RoutingContext;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Path("/api/users")
public class UsersResource {

    @Inject
    SecurityIdentity identity;
    @Inject
    private RoutingContext context;

    @GET
    @Path("/me/bearer")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public User principalName() {
        return new User(identity.getPrincipal().getName());
    }

    @GET
    @Path("/me/bearer-id")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public User principalNameId() {
        return new User(identity.getPrincipal().getName());
    }

    @GET
    @Path("/preferredUserName/bearer")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public User preferredUserName(@QueryParam("includeTenantId") boolean includeTenantId) {
        String preferredUsername = ((JsonWebToken) identity.getPrincipal()).getClaim("preferred_username");
        if (includeTenantId) {
            String tenantId = context.get(TENANT_ID_ATTRIBUTE);
            return new User(preferredUsername, tenantId);
        }
        return new User(preferredUsername);
    }

    @GET
    @Path("/preferredUserName/bearer/token")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public User preferredUserNameWithExtendedPath() {
        String preferredUsername = ((JsonWebToken) identity.getPrincipal()).getClaim("preferred_username");
        String tenantId = context.get(TENANT_ID_ATTRIBUTE);
        return new User(preferredUsername, tenantId);
    }

    @GET
    @Path("/preferredUserName/bearer-wrong-role-path")
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public User preferredUserNameWrongRolePath() {
        return new User(((JsonWebToken) identity.getPrincipal()).getClaim("preferred_username"));
    }
}
