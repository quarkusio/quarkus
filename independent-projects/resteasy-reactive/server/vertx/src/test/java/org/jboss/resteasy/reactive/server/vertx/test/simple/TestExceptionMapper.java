package org.jboss.resteasy.reactive.server.vertx.test.simple;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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
