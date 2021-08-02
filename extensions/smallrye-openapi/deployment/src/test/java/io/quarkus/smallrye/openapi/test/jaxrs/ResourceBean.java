package io.quarkus.smallrye.openapi.test.jaxrs;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;

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
