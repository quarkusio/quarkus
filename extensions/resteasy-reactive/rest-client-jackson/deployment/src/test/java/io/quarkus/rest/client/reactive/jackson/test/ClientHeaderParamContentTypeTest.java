package io.quarkus.rest.client.reactive.jackson.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * A {@code Content-Type} set with {@code @ClientHeaderParam} is sent as is, even when the Jackson writer serializes the
 * body.
 */
public class ClientHeaderParamContentTypeTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class, EchoResource.class, Payload.class))
            .overrideConfigKey("quarkus.rest-client.echo.url", "http://localhost:${quarkus.http.test-port:8081}");

    @RestClient
    Client client;

    @Test
    void staticOverrideIsKept() {
        assertEquals("text/csv", client.sendCsv("a,b"));
    }

    @Test
    void computedOverrideIsKept() {
        assertEquals("multipart/mixed; boundary=batch_42", client.sendBatch("--batch_42"));
    }

    @Test
    void defaultContentTypeIsJson() {
        assertEquals(MediaType.APPLICATION_JSON, client.sendPayload(new Payload("x")));
    }

    @Test
    void consumesIsKept() {
        assertEquals("application/vnd.acme+json", client.sendCustomJson(new Payload("x")));
    }

    @Path("/echo")
    @RegisterRestClient(configKey = "echo")
    public interface Client {

        @POST
        @Path("/content-type")
        @ClientHeaderParam(name = "Content-Type", value = "text/csv")
        String sendCsv(String body);

        @POST
        @Path("/content-type")
        @ClientHeaderParam(name = "Content-Type", value = "{batchContentType}")
        String sendBatch(String body);

        default String batchContentType(String headerName) {
            return "multipart/mixed; boundary=batch_42";
        }

        @POST
        @Path("/content-type")
        String sendPayload(Payload payload);

        @POST
        @Path("/content-type")
        @Consumes("application/vnd.acme+json")
        String sendCustomJson(Payload payload);
    }

    @Path("/echo")
    public static class EchoResource {

        @POST
        @Path("/content-type")
        @Consumes(MediaType.WILDCARD)
        @Produces(MediaType.TEXT_PLAIN)
        public String contentType(@HeaderParam("Content-Type") String contentType, String body) {
            return contentType;
        }
    }

    public static class Payload {
        public String value;

        public Payload() {
        }

        public Payload(String value) {
            this.value = value;
        }
    }
}
