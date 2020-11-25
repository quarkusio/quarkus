package io.quarkus.rest.server.test.resteasy.async.filters;

import javax.annotation.Priority;
import javax.ws.rs.ext.Provider;

@Priority(1)
@Provider
public class AsyncRequestFilter1 extends AsyncRequestFilter {

    public AsyncRequestFilter1() {
        super("Filter1");
    }
}
