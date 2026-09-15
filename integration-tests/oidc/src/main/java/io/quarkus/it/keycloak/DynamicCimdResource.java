package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;

@Path("/tenant-cimd-dynamic")
@Authenticated
public class DynamicCimdResource {

    @GET
    public String cimd() {
        return "hello";
    }

}
