package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import java.net.URI;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@Path("/")
public class UriInfoResolveResource {
    @Produces("text/plain")
    @GET
    @Path("resolve")
    public String relativize(@Context UriInfo info, @QueryParam("to") String to) {
        return info.resolve(URI.create(to)).toString();
    }
}
