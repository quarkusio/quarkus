package io.quarkus.jwt.test;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.jwt.auth.principal.JWTParser;

@Path("/endp")
@DenyAll
@ApplicationScoped
public class DefaultGroupsEndpoint {

    @Inject
    JsonWebToken jwtPrincipal;

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    JWTParser parser;

    @Context
    HttpHeaders headers;

    @GET
    @Path("/echo")
    @RolesAllowed("User")
    public String echoGroups() {
        return jwtPrincipal.getGroups().stream().reduce("", String::concat);
    }

    @GET
    @Path("/routingContext")
    @RolesAllowed("User")
    public String checkRoutingContext() {
        return jwtPrincipal.getGroups().stream().reduce("", String::concat)
                + "; routing-context-available:" + securityIdentity.getAttributes().containsKey("routing-context-available");
    }

    @GET
    @Path("/echo-parser")
    @RolesAllowed("User")
    public String echoGroupsWithParser() throws Exception {
        String rawToken = headers.getHeaderString("Authorization").split(" ")[1].trim();
        return "parser:" + parser.parse(rawToken).getGroups().stream().reduce("", String::concat);
    }
}
