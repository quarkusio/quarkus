package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.Path;

@Path("/collection")
public class ResourceLocatorCollectionResource {

    @Path("annotation_free_subresource")
    public Object getAnnotationFreeSubResource() {
        return new ResourceLocatorAnnotationFreeSubResource();
    }
}
