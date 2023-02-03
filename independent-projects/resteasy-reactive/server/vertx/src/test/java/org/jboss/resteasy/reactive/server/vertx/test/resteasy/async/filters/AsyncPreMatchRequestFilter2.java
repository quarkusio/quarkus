package org.jboss.resteasy.reactive.server.vertx.test.resteasy.async.filters;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

@PreMatching
@Priority(2)
@Provider
public class AsyncPreMatchRequestFilter2 extends AsyncRequestFilter {

    public AsyncPreMatchRequestFilter2() {
        super("PreMatchFilter2");
    }
}
