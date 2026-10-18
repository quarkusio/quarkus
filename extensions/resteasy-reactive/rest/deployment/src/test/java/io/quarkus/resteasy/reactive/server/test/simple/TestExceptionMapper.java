package io.quarkus.resteasy.reactive.server.test.simple;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TestExceptionMapper implements ExceptionMapper<TestException> {

    @Override
    public Response toResponse(TestException exception) {
        return Response.status(666).entity("OK").build();
    }

}
