package io.quarkus.resteasy.runtime;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.core.NoMessageBodyWriterFoundFailure;

@Provider
public class NoMessageBodyWriterFoundFailureExceptionMapper implements ExceptionMapper<NoMessageBodyWriterFoundFailure> {

    @Override
    public Response toResponse(NoMessageBodyWriterFoundFailure exception) {
        return Response.status(exception.getErrorCode()).header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
                .entity(determineMessage(exception)).build();
    }

    private String determineMessage(NoMessageBodyWriterFoundFailure exception) {
        if ((exception.getMessage() != null) && exception.getMessage().contains(MediaType.APPLICATION_JSON)) {
            return exception.getMessage()
                    + ". Consider adding one of the following extensions: 'quarkus-resteasy-jackson', 'quarkus-resteasy-jsonb'";
        }
        return exception.getMessage();
    }
}
