package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.vertx.http.security.AuthorizationPolicy;

@Path("/resource4")
@Tag(name = "test")
@Server(url = "serverUrl")
@SecurityRequirement(name = "AtClassLevel")
public class OpenApiResourceSecuredAtMethodLevel4 {

    @GET
    @Path("/test-security/annotated")
    @AuthorizationPolicy(name = "custom")
    public String secureEndpointWithSecurityAnnotation() {
        return "secret";
    }

}
