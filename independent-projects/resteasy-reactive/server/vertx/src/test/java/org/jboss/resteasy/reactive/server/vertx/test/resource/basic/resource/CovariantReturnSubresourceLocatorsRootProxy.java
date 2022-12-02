package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

public interface CovariantReturnSubresourceLocatorsRootProxy {
    @Path("sub/{path}")
    CovariantReturnSubresourceLocatorsSubProxy getSub(@PathParam("path") String path);
}
