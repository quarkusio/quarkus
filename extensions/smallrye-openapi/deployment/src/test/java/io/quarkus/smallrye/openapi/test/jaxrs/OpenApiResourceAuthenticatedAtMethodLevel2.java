package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.security.Authenticated;

@Path("/resource3")
@Tag(name = "test")
@Server(url = "serverUrl")
@SecurityRequirement(name = "AtClassLevel")
public class OpenApiResourceAuthenticatedAtMethodLevel2 {

    private ResourceBean2 resourceBean;

    @GET
    @Path("/test-security/annotated")
    @Authenticated
    public String secureEndpointWithSecurityAnnotation() {
        return "secret";
    }

}
