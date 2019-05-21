package io.quarkus.it.elytron;

import javax.inject.Inject;
import javax.security.auth.x500.X500Principal;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
