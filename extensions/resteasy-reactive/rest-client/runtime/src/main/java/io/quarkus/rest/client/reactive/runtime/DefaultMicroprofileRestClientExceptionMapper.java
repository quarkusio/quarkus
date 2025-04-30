package io.quarkus.rest.client.reactive.runtime;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.jboss.resteasy.reactive.client.impl.ClientResponseImpl;

@SuppressWarnings("rawtypes")
public class DefaultMicroprofileRestClientExceptionMapper implements ResponseExceptionMapper {

    public Throwable toThrowable(Response response) {
        try {
            response.bufferEntity();
        } catch (Exception ignored) {
        }

        WebApplicationException exception = new WebApplicationException(
                String.format("%s, status code %d", response.getStatusInfo().getReasonPhrase(), response.getStatus()),
                response);

        if (response instanceof ClientResponseImpl clientResponse) {
            StackTraceElement[] callerStackTrace = clientResponse.getCallerStackTrace();
            if (callerStackTrace != null) {
                exception.setStackTrace(callerStackTrace);
            }
        }

        return exception;
    }

    public int getPriority() {
        return Integer.MAX_VALUE;
    }
}
