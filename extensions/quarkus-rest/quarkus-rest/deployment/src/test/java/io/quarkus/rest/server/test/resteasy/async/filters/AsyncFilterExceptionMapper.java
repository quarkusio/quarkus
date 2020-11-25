package io.quarkus.rest.server.test.resteasy.async.filters;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AsyncFilterExceptionMapper implements ExceptionMapper<AsyncFilterException> {

    @Override
    public Response toResponse(AsyncFilterException exception) {
        return Response.ok("exception was mapped").status(Status.ACCEPTED).build();
    }

}
