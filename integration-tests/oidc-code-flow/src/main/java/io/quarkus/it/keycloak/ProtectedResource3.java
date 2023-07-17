package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;

@Path("/web-app3")
@Authenticated
public class ProtectedResource3 {

    @GET
    public String getName() {
        // CodeFlowTest#testAuthenticationCompletionFailedNoStateCookie checks that if a state cookie is missing
        // then 401 is returned when a redirect targets the endpoint requiring authentication
        throw new InternalServerErrorException("This method must not be invoked");
    }
}
