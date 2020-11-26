package io.quarkus.rest.server.test.resteasy.async.filters;

import javax.annotation.Priority;
import javax.ws.rs.ext.Provider;

@Priority(1)
@Provider
public class AsyncResponseFilter1 extends AsyncResponseFilter {

    public AsyncResponseFilter1() {
        super("ResponseFilter1");
    }
}
