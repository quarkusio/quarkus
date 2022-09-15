package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/resource2")
@Tag(name = "test")
@Server(url = "serverUrl")
public class OpenApiResourceSecuredAtMethodLevel {

    private ResourceBean resourceBean;

    @GET
    @Path("/test-security/naked")
    @RolesAllowed("admin")
    public String secureEndpointWithoutSecurityAnnotation() {
        return "secret";
    }

    @GET
    @Path("/test-security/annotated")
    @RolesAllowed("admin")
    @SecurityRequirement(name = "JWTCompanyAuthentication")
    public String secureEndpointWithSecurityAnnotation() {
        return "secret";
    }

    @GET
    @Path("/test-security/methodLevel/1")
    @RolesAllowed("admin")
    public String secureEndpoint1() {
        return "secret";
    }

    @GET
    @Path("/test-security/methodLevel/2")
    @RolesAllowed("admin")
    public String secureEndpoint2() {
        return "secret";
    }

    @GET
    @Path("/test-security/methodLevel/public")
    public String publicEndpoint() {
        return "boo";
    }

    @APIResponses({
            @APIResponse(responseCode = "401", description = "Who are you?"),
            @APIResponse(responseCode = "403", description = "You cannot do that.")
    })
    @GET
    @Path("/test-security/annotated/documented")
    @RolesAllowed("admin")
    @SecurityRequirement(name = "JWTCompanyAuthentication")
    public String secureEndpointWithSecurityAnnotationAndDocument() {
        return "secret";
    }

    @APIResponses({
            @APIResponse(responseCode = "401", description = "Who are you?"),
            @APIResponse(responseCode = "403", description = "You cannot do that.")
    })
    @GET
    @Path("/test-security/methodLevel/3")
    @RolesAllowed("admin")
    public String secureEndpoint3() {
        return "secret";
    }

}
