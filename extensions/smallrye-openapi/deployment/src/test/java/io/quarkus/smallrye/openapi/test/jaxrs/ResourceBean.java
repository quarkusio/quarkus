package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ResourceBean {
    @Override
    public String toString() {
        return "resource";
    }

    @RolesAllowed("admin")
    public String anotherMethod() {
        return "bla";
    }

}
