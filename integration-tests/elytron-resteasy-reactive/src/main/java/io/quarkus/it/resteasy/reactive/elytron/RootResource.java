package io.quarkus.it.resteasy.reactive.elytron;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/")
public class RootResource {
    @Inject
    SecurityIdentity identity;

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public String posts(String data, @Context SecurityContext sec) {
        if (data == null) {
            throw new RuntimeException("No post data");
        }
        if (sec.getUserPrincipal().getName() == null) {
            throw new RuntimeException("Failed to get user principal");
        }
        return "post success";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String approval(@Context SecurityContext sec) {
        if (sec.getUserPrincipal().getName() == null) {
            throw new RuntimeException("Failed to get user principal");
        }
        return "get success";
    }

    @GET
    @Path("/secure")
    @Authenticated
    public String getSecure() {
        return "secure";
    }

    @GET
    @Path("/user")
    @RolesAllowed("user")
    public String user(@Context SecurityContext sec) {
        return sec.getUserPrincipal().getName();
    }

    @GET
    @Path("/attributes")
    @Authenticated
    public String getAttributes() {
        final Map<String, Object> attributes = identity.getAttributes();
        if (attributes == null || attributes.isEmpty()) {
            throw new RuntimeException("No attributes were specified");
        }

        return attributes.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));
    }
}
