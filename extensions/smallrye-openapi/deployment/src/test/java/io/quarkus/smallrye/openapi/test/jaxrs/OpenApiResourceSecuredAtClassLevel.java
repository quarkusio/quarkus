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
@RolesAllowed("admin")
public class OpenApiResourceSecuredAtClassLevel {

    @SuppressWarnings("unused")
    private ResourceBean resourceBean;

    @GET
    @Path("/test-security/classLevel/1")
    @RolesAllowed("user1")
    public String secureEndpoint1() {
        return "secret";
    }

    @GET
    @Path("/test-security/classLevel/2")
    @RolesAllowed("user2")
    public String secureEndpoint2() {
        return "secret";
    }

    @GET
    @Path("/test-security/classLevel/3")
    @SecurityRequirement(name = "MyOwnName")
    public String secureEndpoint3() {
        return "secret";
    }

    @APIResponses({ @APIResponse(responseCode = "401", description = "Who are you?"),
            @APIResponse(responseCode = "403", description = "You cannot do that.") })
    @GET
    @Path("/test-security/classLevel/4")
    public String secureEndpoint4() {
        return "secret";
    }

}
