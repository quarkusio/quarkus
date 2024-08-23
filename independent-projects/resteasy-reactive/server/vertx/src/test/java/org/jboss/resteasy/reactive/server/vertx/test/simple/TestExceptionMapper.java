package org.jboss.resteasy.reactive.server.vertx.test.simple;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TestExceptionMapper implements ExceptionMapper<TestException> {

    public TestExceptionMapper() {
        System.out.println("constructed");
    }

    @Override
    public Response toResponse(TestException exception) {
        return Response.status(666).entity("OK").build();
    }

}
