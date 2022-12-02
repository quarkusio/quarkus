package io.quarkus.it.keycloak;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.security.Authenticated;

@Path("/api2/resource")
@Authenticated
public class ProtectedResource2 {

    @GET
    public String testResource() {
        // This method must not be invoked
        throw new RuntimeException();
    }
}
