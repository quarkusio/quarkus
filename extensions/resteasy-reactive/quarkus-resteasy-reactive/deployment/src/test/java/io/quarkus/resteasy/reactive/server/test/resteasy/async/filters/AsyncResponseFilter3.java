package io.quarkus.resteasy.reactive.server.test.resteasy.async.filters;

import javax.annotation.Priority;
import javax.ws.rs.ext.Provider;

@Priority(3)
@Provider
public class AsyncResponseFilter3 extends AsyncResponseFilter {

    public AsyncResponseFilter3() {
        super("ResponseFilter3");
    }
}
