package io.quarkus.resteasy.runtime;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import io.quarkus.security.AuthenticationCompletionException;

@Provider
public class AuthenticationCompletionExceptionMapper implements ExceptionMapper<AuthenticationCompletionException> {

    @Override
    public Response toResponse(AuthenticationCompletionException ex) {
        return Response.status(401).build();
    }

}
