package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

public interface InheritenceParentResource {

    @GET
    @Produces("text/plain")
    String firstest();

}
