package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

public abstract class ResourceLocatorAbstractAnnotationFreeResouce implements ResourceLocatorRootInterface {

    @Override
    public String get() {
        return "got";
    }
}
