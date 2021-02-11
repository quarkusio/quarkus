package io.quarkus.tck.opentracing;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

/**
 * Temporary fix to catch exceptions thrown in JAX-RS endpoints
 * See https://issues.jboss.org/browse/RESTEASY-1758
 *
 * @author Pavol Loffay
 */
@Provider
public class ExceptionMapper implements javax.ws.rs.ext.ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException exception) {
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
}
