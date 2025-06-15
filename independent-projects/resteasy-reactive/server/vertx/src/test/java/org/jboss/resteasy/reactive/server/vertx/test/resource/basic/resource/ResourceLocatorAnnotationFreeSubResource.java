package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

public class ResourceLocatorAnnotationFreeSubResource extends ResourceLocatorAbstractAnnotationFreeResouce
        implements ResourceLocatorSubInterface {

    @Override
    public String post(String s) {
        return "posted: " + s;
    }

    @Override
    public Object getSubSubResource(String id) {
        return null;
    }
}
