package io.quarkus.jwt.test;

import javax.annotation.security.DenyAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/endp")
@DenyAll
@RequestScoped
public class DefaultGroupsEndpoint {

    @Inject
    JsonWebToken jwtPrincipal;

    @GET
    @Path("/echo")
    @RolesAllowed("User")
    public String echoGroups() {
        return jwtPrincipal.getGroups().stream().reduce("", String::concat);
    }
}
