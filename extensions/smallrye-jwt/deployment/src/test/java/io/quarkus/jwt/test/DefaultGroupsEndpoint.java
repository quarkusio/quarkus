package io.quarkus.jwt.test;

import javax.annotation.security.DenyAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.smallrye.jwt.auth.principal.JWTParser;

@Path("/endp")
@DenyAll
@ApplicationScoped
public class DefaultGroupsEndpoint {

    @Inject
    JsonWebToken jwtPrincipal;

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
    @Path("/echo-parser")
    @RolesAllowed("User")
    public String echoGroupsWithParser() throws Exception {
        String rawToken = headers.getHeaderString("Authorization").split(" ")[1].trim();
        return "parser:" + parser.parse(rawToken).getGroups().stream().reduce("", String::concat);
    }
}
