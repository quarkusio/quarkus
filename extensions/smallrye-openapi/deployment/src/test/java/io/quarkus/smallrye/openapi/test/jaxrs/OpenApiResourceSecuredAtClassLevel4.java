package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.vertx.http.security.AuthorizationPolicy;

@Path("/resource4")
@Tag(name = "test")
@Server(url = "serverUrl")
@AuthorizationPolicy(name = "custom")
public class OpenApiResourceSecuredAtClassLevel4 {

    @GET
    @Path("/test-security/class-level/1")
    public String secureEndpoint1() {
        return "secret";
    }

}
