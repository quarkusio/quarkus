package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;

@Path("/openApiWithSecurity")
public class OpenApiWithSecurity {

    @GET
    @Path("/test-security/naked")
    @Authenticated
    public String secureEndpointWithoutSecurityAnnotation() {
        return "secret";
    }

    @GET
    @Path("/test-security/annotated")
    @RolesAllowed("admin")
    public String secureEndpointWithRolesAllowedAnnotation() {
        return "secret";
    }

    @GET
    @Path("/test-security/annotated2")
    @RolesAllowed("user")
    public String secureEndpointWithRolesAllowed2Annotation() {
        return "secret";
    }

    @GET
    @Path("/test-security/public")
    public String publicEndpoint() {
        return "boo";
    }

}
