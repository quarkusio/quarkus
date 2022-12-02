package io.quarkus.resteasy.reactive.server.test.customproviders;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class UniExceptionMapper implements ExceptionMapper<UniException> {

    @Override
    public Response toResponse(UniException exception) {
        return Response.accepted(exception.getInput()).build();
    }
}
