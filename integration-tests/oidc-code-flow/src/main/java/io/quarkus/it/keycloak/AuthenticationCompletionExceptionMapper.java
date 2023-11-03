package io.quarkus.it.keycloak;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.quarkus.security.AuthenticationCompletionException;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationCompletionExceptionMapper implements ExceptionMapper<AuthenticationCompletionException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(AuthenticationCompletionException exception) {
        return Response.status(401).header("RedirectUri", uriInfo.getAbsolutePath().toString()).build();
    }

}
