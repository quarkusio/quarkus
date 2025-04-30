package io.quarkus.virtual.security.webauthn;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.smallrye.common.annotation.RunOnVirtualThread;

@RunOnVirtualThread
@Path("/")
public class TestResource {
    @Inject
    SecurityIdentity identity;

    @Authenticated
    @Path("secure")
    @GET
    public String getUserName() {
        VirtualThreadsAssertions.assertEverything();
        return identity.getPrincipal().getName() + ": " + identity.getRoles();
    }

    @RolesAllowed("admin")
    @Path("admin")
    @GET
    public String getAdmin() {
        VirtualThreadsAssertions.assertEverything();
        return "OK";
    }

    @RolesAllowed("cheese")
    @Path("cheese")
    @GET
    public String getCheese() {
        VirtualThreadsAssertions.assertEverything();
        return "OK";
    }

    @Path("open")
    @GET
    public String hello() {
        VirtualThreadsAssertions.assertEverything();
        return "Hello";
    }
}
