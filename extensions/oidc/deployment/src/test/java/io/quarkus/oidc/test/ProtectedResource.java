package io.quarkus.oidc.test;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.IdToken;
import io.quarkus.security.Authenticated;

@Path("/protected")
@Authenticated
public class ProtectedResource {

    @Inject
    @IdToken
    JsonWebToken idToken;

    @GET
    public String getName() {
        return idToken.getName();
    }

    @GET
    @Path("tenant/{id}")
    public String getTenantName(@PathParam("id") String tenantId) {
        return tenantId + ":" + idToken.getName();
    }

    @GET
    @Path("logout")
    public void logout() {
        throw new RuntimeException("Logout must be handled by CodeAuthenticationMechanism");
    }
}
