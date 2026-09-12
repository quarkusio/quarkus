package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;

@Path("/tenant-cimd-jwt")
@Authenticated
public class CimdJwtResource {

    @GET
    public String cimdJwt() {
        return "hello";
    }
}
