package io.quarkus.rest.server.runtime.exceptionmappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import io.quarkus.security.AuthenticationCompletionException;

public class AuthenticationCompletionExceptionMapper implements ExceptionMapper<AuthenticationCompletionException> {

    @Override
    public Response toResponse(AuthenticationCompletionException ex) {
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

}
