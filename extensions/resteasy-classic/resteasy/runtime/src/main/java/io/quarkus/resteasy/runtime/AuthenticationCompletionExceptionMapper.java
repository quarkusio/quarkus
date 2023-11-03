package io.quarkus.resteasy.runtime;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import io.quarkus.security.AuthenticationCompletionException;

@Provider
public class AuthenticationCompletionExceptionMapper implements ExceptionMapper<AuthenticationCompletionException> {

    private static final Logger log = Logger.getLogger(AuthenticationCompletionExceptionMapper.class.getName());

    @Override
    public Response toResponse(AuthenticationCompletionException ex) {
        log.debug("Authentication has failed, returning HTTP status 401");
        return Response.status(401).build();
    }

}
