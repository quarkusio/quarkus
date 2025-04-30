package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.security.PermissionsAllowed;

@Path("/resource3")
@Tag(name = "test")
@Server(url = "serverUrl")
@PermissionsAllowed("secure:read")
public class OpenApiResourceSecuredAtClassLevel2 {

    @SuppressWarnings("unused")
    private ResourceBean resourceBean;

    @GET
    @Path("/test-security/classLevel-2/1")
    public String secureEndpoint1() {
        return "secret";
    }

}
