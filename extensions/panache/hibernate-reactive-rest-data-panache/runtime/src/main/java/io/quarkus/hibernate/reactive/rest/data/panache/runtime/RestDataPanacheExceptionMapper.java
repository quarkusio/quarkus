package io.quarkus.hibernate.reactive.rest.data.panache.runtime;

import jakarta.ws.rs.core.Response;

import org.hibernate.HibernateException;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import io.quarkus.rest.data.panache.RestDataPanacheException;
import io.smallrye.mutiny.CompositeException;

public class RestDataPanacheExceptionMapper {

    private static final Logger LOGGER = Logger.getLogger(RestDataPanacheExceptionMapper.class);

    @ServerExceptionMapper({ RestDataPanacheException.class, CompositeException.class })
    public RestResponse<Response> mapExceptions(Exception exception) {
        LOGGER.warnf(exception, "Mapping an unhandled %s", exception.getClass().getSimpleName());
        RestResponse<Response> response = throwableToResponse(exception, exception.getMessage());
        if (response == null) {
            response = RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getMessage());
        }

        return response;
    }

    private RestResponse<Response> throwableToResponse(Throwable throwable, String message) {
        if (throwable instanceof org.hibernate.exception.ConstraintViolationException
                || throwable instanceof HibernateException) {
            return RestResponse.status(Response.Status.CONFLICT.getStatusCode(), message);
        }

        if (throwable instanceof jakarta.validation.ConstraintViolationException) {
            return RestResponse.status(Response.Status.BAD_REQUEST.getStatusCode(), message);
        }

        if (throwable instanceof CompositeException) {
            CompositeException compositeException = (CompositeException) throwable;
            for (Throwable cause : compositeException.getCauses()) {
                RestResponse<Response> response = throwableToResponse(cause, message);
                if (response != null) {
                    return response;
                }
            }

        } else if (throwable.getCause() != null) {
            return throwableToResponse(throwable.getCause(), message);
        }

        return null;
    };
}
