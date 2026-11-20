package io.quarkus.smallrye.openapi.test.jaxrs;

public class AutoTagInterfaceResourceImpl implements AutoTagInterfaceResource {
    @Override
    public String hello() {
        return "hello";
    }
}
