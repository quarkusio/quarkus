package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;

@Path("/service/tenant-public-key")
@Authenticated
public class ServiceProtectedResource {

    @Inject
    JsonWebToken accessToken;

    @GET
    public String getName() {
        return "tenant-public-key" + ":" + accessToken.getName();
    }
}
