package io.quarkus.hibernate.orm.rest.data.panache.runtime;

import javax.persistence.PersistenceException;
import javax.transaction.RollbackException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.hibernate.exception.ConstraintViolationException;

import io.quarkus.arc.ArcUndeclaredThrowableException;
import io.quarkus.rest.data.panache.RestDataPanacheException;

public class RestDataPanacheExceptionMapper implements ExceptionMapper<RestDataPanacheException> {
    @Override
    public Response toResponse(RestDataPanacheException exception) {
        exception.printStackTrace();

        if (exception.getCause() instanceof ArcUndeclaredThrowableException) {
            return toResponse((ArcUndeclaredThrowableException) exception.getCause(), exception.getMessage());
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getMessage()).build();
    }

    private Response toResponse(ArcUndeclaredThrowableException exception, String message) {
        if (exception.getCause() instanceof RollbackException) {
            return toResponse((RollbackException) exception.getCause(), message);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), message).build();
    }

    private Response toResponse(RollbackException exception, String message) {
        if (exception.getCause() instanceof PersistenceException) {
            return toResponse((PersistenceException) exception.getCause(), message);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), message).build();
    }

    private Response toResponse(PersistenceException exception, String message) {
        if (exception.getCause() instanceof ConstraintViolationException) {
            return toResponse((ConstraintViolationException) exception.getCause(), message);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), message).build();
    }

    private Response toResponse(ConstraintViolationException exception, String message) {
        return Response.status(Response.Status.CONFLICT.getStatusCode(), message).build();
    }
}
