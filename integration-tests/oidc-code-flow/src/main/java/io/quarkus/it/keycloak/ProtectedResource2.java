package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.IdToken;
import io.quarkus.security.Authenticated;

@Path("/web-app2")
@Authenticated
public class ProtectedResource2 {

    @Inject
    @IdToken
    JsonWebToken idToken;

    @GET
    @Path("name")
    public String getName() {
        return "web-app2:" + idToken.getName();
    }

    @GET
    @Path("callback-before-redirect")
    public String getNameCallbackBeforeRedirect() {
        throw new InternalServerErrorException("This method must not be invoked");
    }
}
