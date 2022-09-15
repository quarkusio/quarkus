package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;

@Path("/protected")
public class ProtectedResource {

    @GET
    public String echoToken(@HeaderParam("Authorization") String authorization) {
        return authorization.split(" ")[1];
    }
}
