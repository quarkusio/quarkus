package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Produces(MediaType.TEXT_PLAIN)
public interface ResourceLocatorSubInterface extends ResourceLocatorRootInterface {

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    String post(String s);
}
