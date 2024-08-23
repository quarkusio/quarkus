package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/directory")
public class ResourceLocatorDirectory {

    @Path("/receivers/{id}")
    public ResourceLocatorQueueReceiver getReceiver(@PathParam("id") String id) {
        return new ResourceLocatorQueueReceiver();
    }

    @DELETE
    @Path("/receivers/{id}")
    public String closeReceiver(@PathParam("id") String id) throws Exception {
        return ResourceLocatorDirectory.class.getName();
    }
}
