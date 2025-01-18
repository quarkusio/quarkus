package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.security.AuthenticationCompletionException;

public class AuthenticationCompletionExceptionMapper implements ExceptionMapper<AuthenticationCompletionException> {

    @Override
    public Response toResponse(AuthenticationCompletionException ex) {
        if (LaunchMode.isDev() && ex.getMessage() != null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(ex.getMessage()).build();
        }
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

}
