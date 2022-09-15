package io.quarkus.resteasy.runtime;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.quarkus.security.AuthenticationCompletionException;

@Provider
public class AuthenticationCompletionExceptionMapper implements ExceptionMapper<AuthenticationCompletionException> {

    @Override
    public Response toResponse(AuthenticationCompletionException ex) {
        return Response.status(401).build();
    }

}
