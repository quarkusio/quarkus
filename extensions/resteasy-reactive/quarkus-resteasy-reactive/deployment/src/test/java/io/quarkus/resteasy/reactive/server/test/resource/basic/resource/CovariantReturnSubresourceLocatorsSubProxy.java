package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;

public interface CovariantReturnSubresourceLocatorsSubProxy {
    @GET
    @Produces("text/plain")
    String get();
}
