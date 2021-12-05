package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

public abstract class ResourceLocatorAbstractAnnotationFreeResouce implements ResourceLocatorRootInterface {

    public String get() {
        return "got";
    }
}
