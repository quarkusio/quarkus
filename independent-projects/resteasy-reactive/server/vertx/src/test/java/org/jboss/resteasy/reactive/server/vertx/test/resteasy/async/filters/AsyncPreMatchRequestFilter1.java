package org.jboss.resteasy.reactive.server.vertx.test.resteasy.async.filters;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

@PreMatching
@Priority(1)
@Provider
public class AsyncPreMatchRequestFilter1 extends AsyncRequestFilter {

    public AsyncPreMatchRequestFilter1() {
        super("PreMatchFilter1");
    }
}
