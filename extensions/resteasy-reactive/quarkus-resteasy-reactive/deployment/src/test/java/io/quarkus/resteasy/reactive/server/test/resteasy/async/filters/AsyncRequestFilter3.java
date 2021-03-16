package io.quarkus.resteasy.reactive.server.test.resteasy.async.filters;

import javax.annotation.Priority;
import javax.ws.rs.ext.Provider;

@Priority(3)
@Provider
public class AsyncRequestFilter3 extends AsyncRequestFilter {

    public AsyncRequestFilter3() {
        super("Filter3");
    }
}
