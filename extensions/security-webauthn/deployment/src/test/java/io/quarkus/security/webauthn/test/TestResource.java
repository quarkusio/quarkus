package io.quarkus.security.webauthn.test;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/")
public class TestResource {
    @Inject
    SecurityIdentity identity;

    @Authenticated
    @Path("secure")
    @GET
    public String getUserName() {
        return identity.getPrincipal().getName() + ": " + identity.getRoles();
    }

    @RolesAllowed("admin")
    @Path("admin")
    @GET
    public String getAdmin() {
        return "OK";
    }

    @RolesAllowed("cheese")
    @Path("cheese")
    @GET
    public String getCheese() {
        return "OK";
    }

    @Path("open")
    @GET
    public String hello() {
        return "Hello";
    }

}