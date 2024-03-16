package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import io.quarkus.security.AuthenticationCompletionException;

public class AuthenticationCompletionExceptionMapper implements ExceptionMapper<AuthenticationCompletionException> {

    @Override
    public Response toResponse(AuthenticationCompletionException ex) {
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

}
