package io.quarkus.amazon.lambda.http.test.provider;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class CustomExceptionMapper implements ExceptionMapper<UnsupportedOperationException> {

    public CustomExceptionMapper() {
        System.out.println("Starting custom exception mapper");
    }

    @Override
    public Response toResponse(UnsupportedOperationException throwable) {
        // This class was modified a bit to work properly with RESTEasy
        return Response.status(Response.Status.NOT_IMPLEMENTED)
                .entity(throwable.getMessage())
                .type(MediaType.TEXT_PLAIN).build();
    }
}
