package io.quarkus.rest.client.reactive.runtime;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class DefaultMicroprofileRestClientExceptionMapper implements ResponseExceptionMapper {

    public Throwable toThrowable(Response response) {
        try {
            response.bufferEntity();
        } catch (Exception var3) {
        }

        return new WebApplicationException(
                String.format("%s, status code %d", response.getStatusInfo().getReasonPhrase(), response.getStatus()),
                response);
    }

    public boolean handles(int status, MultivaluedMap headers) {
        return status >= 400;
    }

    public int getPriority() {
        return Integer.MAX_VALUE;
    }
}
