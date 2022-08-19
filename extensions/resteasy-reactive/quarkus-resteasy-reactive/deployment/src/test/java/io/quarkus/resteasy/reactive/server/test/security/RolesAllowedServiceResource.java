package io.quarkus.resteasy.reactive.server.test.security;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
