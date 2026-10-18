package io.quarkus.resteasy.reactive.server.test.security;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.jboss.resteasy.reactive.RestCookie;
import org.jboss.resteasy.reactive.RestHeader;
import org.jboss.resteasy.reactive.RestPath;

import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.smallrye.common.annotation.NonBlocking;

@Path("/permissions")
public class PermissionsAllowedResource {

    @Inject
    CurrentIdentityAssociation currentIdentityAssociation;

    @POST
    @PermissionsAllowed("create")
    @PermissionsAllowed("update")
    public String createOrUpdate() {
        return "done";
    }

    @Path("/admin")
    @PermissionsAllowed("read:resource-admin")
    @GET
    public String admin() {
        return "admin";
    }

    @NonBlocking
    @Path("/admin/security-identity")
    @PermissionsAllowed("get-identity")
    @GET
    public String getSecurityIdentity() {
        return currentIdentityAssociation.getIdentity().getPrincipal().getName();
    }

    @PermissionsAllowed(value = "perm1", permission = CustomPermission.class)
    @Path("/custom-permission")
    @GET
    public String greetings(@QueryParam("greeting") String greeting) {
        return greeting;
    }

    @PermissionsAllowed(value = "farewell", permission = CustomPermissionWithExtraArgs.class, params = { "goodbye", "toWhom",
            "day", "place" })
    @Path("/custom-perm-with-args/{goodbye}")
    @POST
    public String farewell(@RestPath String goodbye, @RestHeader("toWhom") String toWhom, @RestCookie int day, String place) {
        String farewell = String.join(" ", new String[] { goodbye, toWhom, Integer.toString(day), place });
        return farewell;
    }

    @CreateOrUpdate
    @Path("/custom-perm-with-args-meta-annotation/{goodbye}")
    @POST
    public String farewellMetaAnnotation(@RestPath String goodbye, @RestHeader("toWhom") String toWhom, @RestCookie int day,
            String place) {
        String farewell = String.join(" ", new String[] { goodbye, toWhom, Integer.toString(day), place });
        return farewell;
    }
}
