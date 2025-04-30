package io.quarkus.resteasy.reactive.server.test.security;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.SecurityContext;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.smallrye.mutiny.Uni;

@Path("permission-checkers")
public class PermissionCheckersResource {

    @PermissionsAllowed("worker-thread")
    @GET
    @Path("worker-thread")
    public String workerThread(SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

    @PermissionsAllowed("io-thread")
    @GET
    @Path("io-thread")
    public String ioThread(SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

    @PermissionsAllowed("io-thread-uni")
    @GET
    @Path("io-thread-uni")
    public String ioThreadUni(SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

    @PermissionsAllowed("worker-thread-method-args")
    @GET
    @Path("worker-thread-method-args")
    public String workerThreadMethodArgs(SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

    @PermissionsAllowed("io-thread-method-args")
    @GET
    @Path("io-thread-method-args")
    public Uni<String> ioThreadMethodArgs(SecurityContext securityContext) {
        return Uni.createFrom().item(securityContext.getUserPrincipal().getName());
    }

    @PermissionsAllowed("checker-inside-resource")
    @GET
    @Path("checker-inside-resource")
    public String checkerInsideResource(SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

    @PermissionChecker("checker-inside-resource")
    boolean hasAdminRole(SecurityContext securityContext) {
        return securityContext.isUserInRole("admin");
    }
}
