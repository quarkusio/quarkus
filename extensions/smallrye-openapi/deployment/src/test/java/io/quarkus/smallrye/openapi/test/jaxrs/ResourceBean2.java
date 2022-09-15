package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.Authenticated;

@ApplicationScoped
public class ResourceBean2 {
    @Override
    public String toString() {
        return "resource";
    }

    @Authenticated
    public String anotherMethod() {
        return "bla";
    }

}
