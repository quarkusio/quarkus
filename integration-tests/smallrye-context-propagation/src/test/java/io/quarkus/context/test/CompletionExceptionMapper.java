package io.quarkus.context.test;

import java.util.concurrent.CompletionException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;

/**
 * Workaround of https://github.com/resteasy/Resteasy/pull/2001
 * Remove when released.
 */
@Provider
public class CompletionExceptionMapper implements ExceptionMapper<CompletionException> {

    @Context
    Providers providers;

    @Override
    public Response toResponse(CompletionException exception) {
        Throwable cause = exception.getCause();
        if (cause != null) {
            ExceptionMapper<Throwable> mapper = (ExceptionMapper<Throwable>) providers.getExceptionMapper(cause.getClass());
            if (mapper != null)
                return mapper.toResponse(cause);
            if (cause instanceof WebApplicationException)
                return ((WebApplicationException) cause).getResponse();
        }
        System.err.println("Could not find mapper for completion exception");
        exception.printStackTrace();
        return Response.serverError().build();
    }

}
