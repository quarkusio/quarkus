package io.quarkus.it.jackson;

import java.io.IOException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.runtime.annotations.RegisterForReflection;
import tools.jackson.databind.ObjectMapper;

@Path("/throwable")
@Produces(MediaType.APPLICATION_JSON)
public class ThrowableResource {

    private final ObjectMapper objectMapper;

    public ThrowableResource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GET
    public Response get() throws IOException {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(objectMapper.writeValueAsString(exception("E1", "Invalid request"))).build();
    }

    @GET
    @Path("/graph")
    public Response graph() throws IOException {
        TestException exception = exception("E1", "Invalid request");
        exception.initCause(exception("E2", "Cause"));
        exception.addSuppressed(exception("E3", "Suppressed"));
        return Response.status(Response.Status.BAD_REQUEST).entity(objectMapper.writeValueAsString(exception)).build();
    }

    private static TestException exception(String code, String message) {
        TestException exception = new TestException(code, message);
        exception.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("org.acme.Service", "execute", "Service.java", 42)
        });
        return exception;
    }

    @RegisterForReflection
    public static class TestException extends RuntimeException {

        private final String code;

        public TestException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
