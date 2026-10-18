package io.quarkus.mongodb.rest.data.panache.runtime;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.jboss.logging.Logger;

import com.mongodb.MongoWriteException;

import io.quarkus.rest.data.panache.RestDataPanacheException;

public class RestDataPanacheExceptionMapper implements ExceptionMapper<RestDataPanacheException> {

    private static final String DUPLICATE_KEY_ERROR_CODE = "E11000";

    private static final Logger LOGGER = Logger.getLogger(RestDataPanacheExceptionMapper.class);

    @Override
    public Response toResponse(RestDataPanacheException exception) {
        LOGGER.warnf(exception, "Mapping an unhandled %s", RestDataPanacheException.class.getSimpleName());
        return throwableToResponse(exception, exception.getMessage());
    }

    private Response throwableToResponse(Throwable throwable, String message) {
        if (throwable instanceof MongoWriteException && throwable.getMessage().contains(DUPLICATE_KEY_ERROR_CODE)) {
            return Response.status(Response.Status.CONFLICT.getStatusCode(), message).build();
        }

        if (throwable.getCause() != null) {
            return throwableToResponse(throwable.getCause(), message);
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), message).build();
    };
}
