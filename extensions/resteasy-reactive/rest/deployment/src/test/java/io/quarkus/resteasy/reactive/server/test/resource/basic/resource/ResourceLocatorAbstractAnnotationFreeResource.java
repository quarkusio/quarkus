package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

public abstract class ResourceLocatorAbstractAnnotationFreeResource implements ResourceLocatorRootInterface {

    @Override
    public String get() {
        return "got";
    }
}
