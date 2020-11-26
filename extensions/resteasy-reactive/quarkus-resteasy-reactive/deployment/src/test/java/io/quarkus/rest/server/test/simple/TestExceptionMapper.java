package io.quarkus.rest.server.test.simple;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class TestExceptionMapper implements ExceptionMapper<TestException> {

    @Override
    public Response toResponse(TestException exception) {
        return Response.status(666).entity("OK").build();
    }

}
