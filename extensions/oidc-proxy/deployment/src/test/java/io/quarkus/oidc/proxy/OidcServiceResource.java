package io.quarkus.oidc.proxy;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;

@Path("/service")
@Authenticated
public class OidcServiceResource {

    @Inject
    JsonWebToken accessToken;

    @GET
    @Produces("text/plain")
    public String getName() {
        return accessToken.getClaim("typ") + " " + accessToken.getName();
    }
}
