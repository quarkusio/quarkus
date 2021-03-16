package io.quarkus.it.keycloak;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import io.quarkus.security.Authenticated;

@Path("/tenant-https")
@Authenticated
public class TenantHttps {

    @GET
    public String getTenant() {
        return "tenant-https";
    }

    @GET
    @Path("query")
    public String getTenantWithQuery(@QueryParam("a") String value) {
        return "tenant-https?a=" + value;
    }
}
