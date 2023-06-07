package io.quarkus.rest.client.reactive.runtime;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.resteasy.reactive.client.impl.ClientResponseImpl;

public class DefaultMicroprofileRestClientExceptionMapper implements ResponseExceptionMapper {

    public Throwable toThrowable(Response response) {
        try {
            response.bufferEntity();
        } catch (Exception ignored) {
        }

        WebApplicationException exception = new WebApplicationException(
                String.format("%s, status code %d", response.getStatusInfo().getReasonPhrase(), response.getStatus()),
                response);

        if (response instanceof ClientResponseImpl) {
            ClientResponseImpl clientResponse = (ClientResponseImpl) response;
            StackTraceElement[] callerStackTrace = clientResponse.getCallerStackTrace();
            if (callerStackTrace != null) {
                exception.setStackTrace(callerStackTrace);
            }
        }

        return exception;
    }

    public boolean handles(int status, MultivaluedMap headers) {
        return status >= 400;
    }

    public int getPriority() {
        return Integer.MAX_VALUE;
    }
}
