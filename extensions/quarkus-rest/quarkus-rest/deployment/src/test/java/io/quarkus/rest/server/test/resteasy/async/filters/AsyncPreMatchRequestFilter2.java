package io.quarkus.rest.server.test.resteasy.async.filters;

import javax.annotation.Priority;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

@PreMatching
@Priority(2)
@Provider
public class AsyncPreMatchRequestFilter2 extends AsyncRequestFilter {

    public AsyncPreMatchRequestFilter2() {
        super("PreMatchFilter2");
    }
}
