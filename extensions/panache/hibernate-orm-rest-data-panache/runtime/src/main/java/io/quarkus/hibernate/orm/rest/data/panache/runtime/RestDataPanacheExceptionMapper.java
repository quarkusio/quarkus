package io.quarkus.hibernate.orm.rest.data.panache.runtime;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.jboss.logging.Logger;

import io.quarkus.rest.data.panache.RestDataPanacheException;

public class RestDataPanacheExceptionMapper implements ExceptionMapper<RestDataPanacheException> {
    private static final Logger LOGGER = Logger.getLogger(RestDataPanacheExceptionMapper.class);

    @Override
    public Response toResponse(RestDataPanacheException exception) {
        LOGGER.warnf(exception, "Mapping an unhandled %s", RestDataPanacheException.class.getSimpleName());
        return throwableToResponse(exception, exception.getMessage());
    }

    private Response throwableToResponse(Throwable throwable, String message) {
        if (throwable instanceof org.hibernate.exception.ConstraintViolationException) {
            return Response.status(Response.Status.CONFLICT.getStatusCode(), message).build();
        }

        if (throwable instanceof jakarta.validation.ConstraintViolationException) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), message).build();
        }

        if (throwable.getCause() != null) {
            return throwableToResponse(throwable.getCause(), message);
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), message).build();
    };
}
