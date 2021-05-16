package io.quarkus.it.keycloak;

import io.quarkus.security.Authenticated;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/api2/resource")
@Authenticated
public class ProtectedResource2 {

    @GET
    public String testResource() {
        // This method must not be invoked
        throw new RuntimeException();
    }
}
