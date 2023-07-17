package org.jboss.resteasy.reactive.server.vertx.test.customproviders;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class UniExceptionMapper implements ExceptionMapper<UniException> {

    @Override
    public Response toResponse(UniException exception) {
        return Response.accepted(exception.getInput()).build();
    }
}
