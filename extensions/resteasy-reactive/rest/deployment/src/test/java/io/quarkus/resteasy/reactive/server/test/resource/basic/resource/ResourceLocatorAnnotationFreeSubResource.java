package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

public class ResourceLocatorAnnotationFreeSubResource extends ResourceLocatorAbstractAnnotationFreeResource
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
