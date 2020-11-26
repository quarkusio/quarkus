package io.quarkus.rest.server.test.resteasy.async.filters;

import javax.annotation.Priority;
import javax.ws.rs.ext.Provider;

@Priority(2)
@Provider
public class AsyncRequestFilter2 extends AsyncRequestFilter {

    public AsyncRequestFilter2() {
        super("Filter2");
    }
}
