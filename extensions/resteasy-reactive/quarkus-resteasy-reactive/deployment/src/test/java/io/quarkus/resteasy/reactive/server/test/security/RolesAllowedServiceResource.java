package io.quarkus.resteasy.reactive.server.test.security;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/roles-service")
public class RolesAllowedServiceResource {

    @Inject
    RolesAllowedService rolesAllowedService;

    @Path("/hello")
    @RolesAllowed({ "user", "admin" })
    @GET
    public String getServiceHello() {
        return rolesAllowedService.hello();
    }

}
