package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

public class ResourceLocatorAnnotationFreeSubResource extends ResourceLocatorAbstractAnnotationFreeResouce
        implements ResourceLocatorSubInterface {

    public String post(String s) {
        return "posted: " + s;
    }

    public Object getSubSubResource(String id) {
        return null;
    }
}
