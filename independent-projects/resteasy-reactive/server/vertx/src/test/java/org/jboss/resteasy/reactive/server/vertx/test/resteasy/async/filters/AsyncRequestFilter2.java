package org.jboss.resteasy.reactive.server.vertx.test.resteasy.async.filters;

import jakarta.annotation.Priority;
import jakarta.ws.rs.ext.Provider;

@Priority(2)
@Provider
public class AsyncRequestFilter2 extends AsyncRequestFilter {

    public AsyncRequestFilter2() {
        super("Filter2");
    }
}
