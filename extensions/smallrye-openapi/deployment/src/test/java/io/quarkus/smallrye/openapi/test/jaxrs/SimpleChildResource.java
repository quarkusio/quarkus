package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Path;

@Path("/simple-child")
public class SimpleChildResource extends SimpleBaseResource {

    @RolesAllowed("admin")
    @Override
    public String simpleMethod() {
        return super.simpleMethod();
    }
}
