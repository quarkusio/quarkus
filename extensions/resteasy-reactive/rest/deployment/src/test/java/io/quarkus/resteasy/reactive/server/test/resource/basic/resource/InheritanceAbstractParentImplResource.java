package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

public class InheritanceAbstractParentImplResource extends InheritanceAbstractParentResource {

    @Override
    public String get() {
        return "works";
    }

}
