package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import jakarta.ws.rs.Path;

@Path("/collection")
public class ResourceLocatorCollectionResource {

    @Path("annotation_free_subresource")
    public Object getAnnotationFreeSubResource() {
        return new ResourceLocatorAnnotationFreeSubResource();
    }
}
