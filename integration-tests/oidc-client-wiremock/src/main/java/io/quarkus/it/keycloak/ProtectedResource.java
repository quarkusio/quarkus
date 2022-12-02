package io.quarkus.it.keycloak;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;

@Path("/protected")
public class ProtectedResource {

    @GET
    public String echoToken(@HeaderParam("Authorization") String authorization) {
        return authorization.split(" ")[1];
    }
}
