package io.quarkus.it.keycloak;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.quarkus.security.AuthenticationCompletionException;

@Provider
public class CodeFlowIssuerExceptionMapper implements ExceptionMapper<AuthenticationCompletionException> {

    @Override
    public Response toResponse(AuthenticationCompletionException exception) {
        return Response.status(401).entity(exception.getMessage()).build();
    }
}
