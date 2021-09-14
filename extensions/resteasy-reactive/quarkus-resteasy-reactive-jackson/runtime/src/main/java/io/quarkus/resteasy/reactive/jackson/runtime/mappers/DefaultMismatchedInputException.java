package io.quarkus.resteasy.reactive.jackson.runtime.mappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class DefaultMismatchedInputException
        implements ExceptionMapper<com.fasterxml.jackson.databind.exc.MismatchedInputException> {

    @Override
    public Response toResponse(com.fasterxml.jackson.databind.exc.MismatchedInputException exception) {
        return Response.status(Response.Status.BAD_REQUEST).build();
    }
}
