package io.quarkus.resteasy.reactive.server.test.resteasy.async.filters;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class AsyncFilterExceptionMapper implements ExceptionMapper<AsyncFilterException> {

    @Override
    public Response toResponse(AsyncFilterException exception) {
        return Response.ok("exception was mapped").status(Status.ACCEPTED).build();
    }

}
