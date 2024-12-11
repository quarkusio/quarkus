package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.security.Authenticated;

@Path("/resource-inherited2")
@Tag(name = "test")
@Server(url = "serverUrl")
@Authenticated
public class OpenApiResourceAuthenticatedInherited2 extends OpenApiResourceAuthenticatedInherited1 {

    @GET
    @Path("/test-security/classLevel/2")
    @SecurityRequirement(name = "CustomOverride")
    public String secureEndpoint2() {
        return "secret";
    }

}
