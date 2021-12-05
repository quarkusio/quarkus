package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import javax.ws.rs.Path;

@Path("/collection")
public class ResourceLocatorCollectionResource {

    @Path("annotation_free_subresource")
    public Object getAnnotationFreeSubResource() {
        return new ResourceLocatorAnnotationFreeSubResource();
    }
}
