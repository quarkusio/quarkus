package io.quarkus.it.keycloak;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.security.Authenticated;

@Path("/tenant-https")
public class TenantHttps {

    @Authenticated
    @GET
    public String getTenant() {
        return "tenant-https";
    }
}
