package org.jboss.resteasy.reactive.client.impl;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.RedirectionException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status.Family;

class ExceptionUtil {

    private ExceptionUtil() {
    }

    static WebApplicationException toException(String message, Response response,
            Throwable cause) {
        Response.Status status = response.getStatusInfo().toEnum();
        if (status == null) {
            return createExceptionForFamily(message, response, cause);
        } else {
            return switch (status) {
                case BAD_REQUEST -> new BadRequestException(message, response, cause);
                case UNAUTHORIZED -> new NotAuthorizedException(message, response, cause);
                case FORBIDDEN -> new ForbiddenException(message, response, cause);
                case NOT_FOUND -> new NotFoundException(message, response, cause);
                case METHOD_NOT_ALLOWED -> new NotAllowedException(message, response, cause);
                case NOT_ACCEPTABLE -> new NotAcceptableException(message, response, cause);
                case UNSUPPORTED_MEDIA_TYPE -> new NotSupportedException(message, response, cause);
                case INTERNAL_SERVER_ERROR -> new InternalServerErrorException(message, response, cause);
                case SERVICE_UNAVAILABLE -> new ServiceUnavailableException(message, response, cause);
                default -> createExceptionForFamily(message, response, cause);
            };
        }
    }

    private static WebApplicationException createExceptionForFamily(String message,
            Response response, Throwable cause) {
        Family statusFamily = response.getStatusInfo().getFamily();
        return switch (statusFamily) {
            case REDIRECTION -> new RedirectionException(message, response);
            case CLIENT_ERROR -> new ClientErrorException(message, response, cause);
            case SERVER_ERROR -> new ServerErrorException(message, response, cause);
            default -> new WebApplicationException(message, cause, response);
        };
    }
}
