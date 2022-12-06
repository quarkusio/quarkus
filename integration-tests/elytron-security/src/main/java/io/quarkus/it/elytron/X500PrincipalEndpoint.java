package io.quarkus.it.elytron;

import javax.security.auth.x500.X500Principal;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/x500")
public class X500PrincipalEndpoint {
    @Inject
    X500Principal x500Principal;

    @GET
    @Path("/verifyInjectedPrincipal")
    @Produces(MediaType.TEXT_PLAIN)
    public String verifyInjectedPrincipal() {
        String name = x500Principal == null ? "anonymous" : x500Principal.getName();
        return name;
    }
}
