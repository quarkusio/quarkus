package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.security.Authenticated;

@Path("/resource2")
@Tag(name = "test")
@Server(url = "serverUrl")
public class OpenApiResourceAuthenticatedAtMethodLevel {

    private ResourceBean2 resourceBean;

    @GET
    @Path("/test-security/naked")
    @Authenticated
    public String secureEndpointWithoutSecurityAnnotation() {
        return "secret";
    }

    @GET
    @Path("/test-security/annotated")
    @Authenticated
    @SecurityRequirement(name = "JWTCompanyAuthentication")
    public String secureEndpointWithSecurityAnnotation() {
        return "secret";
    }

    @GET
    @Path("/test-security/methodLevel/1")
    @Authenticated
    public String secureEndpoint1() {
        return "secret";
    }

    @GET
    @Path("/test-security/methodLevel/2")
    @Authenticated
    public String secureEndpoint2() {
        return "secret";
    }

    @GET
    @Path("/test-security/methodLevel/public")
    public String publicEndpoint() {
        return "boo";
    }

}
