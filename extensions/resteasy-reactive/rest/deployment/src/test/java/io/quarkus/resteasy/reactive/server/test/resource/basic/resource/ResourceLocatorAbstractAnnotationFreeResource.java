package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

public abstract class ResourceLocatorAbstractAnnotationFreeResource implements ResourceLocatorRootInterface {

    public String get() {
        return "got";
    }
}
