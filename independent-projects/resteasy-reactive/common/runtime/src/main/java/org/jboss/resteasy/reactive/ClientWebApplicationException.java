package org.jboss.resteasy.reactive;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class ClientWebApplicationException extends WebApplicationException implements ResteasyReactiveClientProblem {

    public ClientWebApplicationException() {
        super();
    }

    public ClientWebApplicationException(String message) {
        super(message);
    }

    public ClientWebApplicationException(Response response) {
        super(response);
    }

    public ClientWebApplicationException(String message, Response response) {
        super(message, response);
    }

    public ClientWebApplicationException(int status) {
        super(status);
    }

    public ClientWebApplicationException(String message, int status) {
        super(message, status);
    }

    public ClientWebApplicationException(Response.Status status) {
        super(status);
    }

    public ClientWebApplicationException(String message, Response.Status status) {
        super(message, status);
    }

    public ClientWebApplicationException(Throwable cause) {
        super(cause);
    }

    public ClientWebApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientWebApplicationException(Throwable cause, Response response) {
        super(cause, response);
    }

    public ClientWebApplicationException(String message, Throwable cause, Response response) {
        super(message, cause, response);
    }

    public ClientWebApplicationException(Throwable cause, int status) {
        super(cause, status);
    }

    public ClientWebApplicationException(String message, Throwable cause, int status) {
        super(message, cause, status);
    }

    public ClientWebApplicationException(Throwable cause, Response.Status status) throws IllegalArgumentException {
        super(cause, status);
    }

    public ClientWebApplicationException(String message, Throwable cause, Response.Status status)
            throws IllegalArgumentException {
        super(message, cause, status);
    }
}
