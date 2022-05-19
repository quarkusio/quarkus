package org.acme.security.keycloak.authorization;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/api/admin")
public class AdminResource {

    @GET
    public String manage() {
        return "granted";
    }
}
