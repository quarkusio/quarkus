package io.quarkus.it.keycloak;

import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;

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
