package io.quarkus.it.keycloak;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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
