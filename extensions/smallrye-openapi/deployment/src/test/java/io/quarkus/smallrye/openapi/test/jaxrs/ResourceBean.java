package io.quarkus.smallrye.openapi.test.jaxrs;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ResourceBean {
    @Override
    public String toString() {
        return "resource";
    }
}
