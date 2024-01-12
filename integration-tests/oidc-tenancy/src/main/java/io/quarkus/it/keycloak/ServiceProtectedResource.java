package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;
import io.quarkus.security.PermissionsAllowed;

@Path("/service/tenant-public-key")
@Authenticated
public class ServiceProtectedResource {

    @Inject
    JsonWebToken accessToken;

    @GET
    @PermissionsAllowed("read:data")
    public String getName() {
        return "tenant-public-key" + ":" + accessToken.getName();
    }
}
