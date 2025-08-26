package io.quarkus.rest.client.reactive.jackson.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class RereadEntityInExceptionMapperTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(
                    Client.class, RereadingExceptionMapper.class, CustomException.class, ErrorMessage.class));

    @TestHTTPResource
    URI uri;

    @Test
    public void testFormattedResponse() {
        Client client = createClient();

        try {
            client.throwExceptionWithCause();
            fail("should have thrown an exception");
        } catch (CustomException e) {
            assertEquals(404, e.getStatus().getStatusCode());
            assertEquals("ROOT_CAUSE_MESSAGE", e.getMessage());
            ErrorMessage msg = e.getErrorMessage();
            assertEquals("RANDOM_EXCEPTION", msg.code());
        }
    }

    @Test
    public void testStringResponse() {
        Client client = createClient();

        try {
            client.throwException500WithStringBody();
            fail("should have thrown an exception");
        } catch (CustomException e) {
            assertEquals(500, e.getStatus().getStatusCode());
            assertEquals("UNMANAGED ERROR RETURNING STRING", e.getMessage());
            ErrorMessage msg = e.getErrorMessage();
            assertEquals("UNKNOWN", msg.code());
        }
    }

    private Client createClient() {
        return RestClientBuilder.newBuilder()
                .baseUri(uri)
                .build(Client.class);
    }

    @Path("/test")
    @RegisterProvider(value = RereadingExceptionMapper.class)
    public interface Client {

        @Produces(MediaType.APPLICATION_JSON)
        @Path("/500StringBody")
        @GET
        Response throwException500WithStringBody();

        @Produces(MediaType.APPLICATION_JSON)
        @Path("/withCause")
        @GET
        Response throwExceptionWithCause();
    }

    public static class RereadingExceptionMapper implements ResponseExceptionMapper<CustomException> {

        @Override
        public CustomException toThrowable(Response response) {
            int statusInt = response.getStatus();
            Response.Status status = Response.Status.fromStatusCode(statusInt);

            ErrorMessage error = null;
            try {
                //necessary to read the entity potentially multiple times
                response.bufferEntity();
                error = response.readEntity(ErrorMessage.class);
            } catch (Exception ignored) {

            }

            if (error != null) {
                return new CustomException(error.code(), error.message(), status);
            } else {
                //read body as string if ProcessingException has been raised
                String msg;
                try {
                    msg = response.readEntity(String.class);
                } catch (Exception e) {
                    if (e.getCause() instanceof IOException) {
                        msg = e.getCause().getMessage();
                    } else {
                        msg = e.getMessage();
                    }
                }
                return new CustomException("UNKNOWN", msg, status);
            }
        }

    }

    public static class CustomException extends RuntimeException {

        public final ErrorMessage errorMessage;
        public final Response.Status status;

        public CustomException(String code, String message, Response.Status status) {
            this(code, message, status, null);
        }

        public CustomException(String code, String message, Response.Status status, Throwable cause) {
            this(new ErrorMessage(code, message), status, cause);
        }

        public CustomException(ErrorMessage errorMessage, Response.Status status, Throwable cause) {
            super(errorMessage.message(), cause);
            this.errorMessage = errorMessage;
            this.status = status;
        }

        public ErrorMessage getErrorMessage() {
            return errorMessage;
        }

        public Response.Status getStatus() {
            return status;
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            // we don't need the stacktrace, so let's remove the clutter from the CI logs
            return this;
        }
    }

    public record ErrorMessage(String code, String message) {

    }

    @Path("/test")
    public static class Resource {

        @Produces(MediaType.APPLICATION_JSON)
        @Path("/withCause")
        @GET
        public Response throwExceptionWithCause() {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(new ErrorMessage("RANDOM_EXCEPTION", "ROOT_CAUSE_MESSAGE"))
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        }

        @Produces(MediaType.APPLICATION_JSON)
        @Path("/500StringBody")
        @GET
        public Response throwException500WithStringBody() {
            return Response.serverError().entity("UNMANAGED ERROR RETURNING STRING").build();
        }

    }

}
