package io.quarkus.rest.server.test.resteasy.async.filters;

import javax.annotation.Priority;
import javax.ws.rs.ext.Provider;

@Priority(2)
@Provider
public class AsyncResponseFilter2 extends AsyncResponseFilter {

    public AsyncResponseFilter2() {
        super("ResponseFilter2");
    }
}
