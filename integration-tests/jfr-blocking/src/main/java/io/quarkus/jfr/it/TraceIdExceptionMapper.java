package io.quarkus.jfr.it;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TraceIdExceptionMapper implements ExceptionMapper<JfrTestException> {
    @Override
    public Response toResponse(JfrTestException e) {
        return Response.serverError().entity(e.getMessage()).build();
    }
}
