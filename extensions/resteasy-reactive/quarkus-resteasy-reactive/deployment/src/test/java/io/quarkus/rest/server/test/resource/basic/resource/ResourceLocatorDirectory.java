package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

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
