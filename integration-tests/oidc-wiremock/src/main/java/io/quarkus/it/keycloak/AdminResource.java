package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.security.identity.SecurityIdentity;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Path("/api/admin")
public class AdminResource {

    @Inject
    SecurityIdentity identity;

    @Path("bearer")
    @GET
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public String admin() {
        return "granted:" + identity.getRoles();
    }

    @Path("bearer-no-introspection")
    @GET
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public String adminNoIntrospection() {
        return "granted:" + identity.getRoles();
    }

    @Path("bearer-role-claim-path")
    @GET
    @RolesAllowed("custom")
    @Produces(MediaType.APPLICATION_JSON)
    public String adminCustomRolePath() {
        return "granted:" + identity.getRoles();
    }

    @Path("bearer-key-without-kid-thumbprint")
    @GET
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public String adminNoKidandThumprint() {
        return "granted:" + identity.getRoles();
    }

    @Path("bearer-wrong-role-path")
    @GET
    @RolesAllowed("admin")
    @Produces(MediaType.APPLICATION_JSON)
    public String adminWrongRolePath() {
        return "granted:" + identity.getRoles();
    }
}
