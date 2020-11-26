package io.quarkus.rest.server.runtime.exceptionmappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import io.quarkus.security.ForbiddenException;

public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {

    @Override
    public Response toResponse(ForbiddenException exception) {
        return Response.status(Response.Status.FORBIDDEN).build();
    }
}
