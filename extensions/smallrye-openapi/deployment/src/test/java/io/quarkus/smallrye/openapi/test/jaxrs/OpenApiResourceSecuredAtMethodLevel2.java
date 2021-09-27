package io.quarkus.smallrye.openapi.test.jaxrs;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/resource3")
@Tag(name = "test")
@Server(url = "serverUrl")
@SecurityRequirement(name = "AtClassLevel")
public class OpenApiResourceSecuredAtMethodLevel2 {

    private ResourceBean resourceBean;

    @GET
    @Path("/test-security/annotated")
    @RolesAllowed("admin")
    public String secureEndpointWithSecurityAnnotation() {
        return "secret";
    }

}
