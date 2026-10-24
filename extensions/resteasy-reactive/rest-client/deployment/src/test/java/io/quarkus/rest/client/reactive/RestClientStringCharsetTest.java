package io.quarkus.rest.client.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class RestClientStringCharsetTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(Client.class, Resource.class));

    @TestHTTPResource
    URI uri;

    private static final String SPECIAL_TEXT = "héllo wörld €";

    private Client client() {
        return RestClientBuilder.newBuilder()
                .baseUri(uri)
                .build(Client.class);
    }

    @Test
    void shouldEncodeStringUsingIso88591() {
        Client client = client();
        byte[] bytes = client.postStringIso("héllo");
        assertThat(bytes).isEqualTo("héllo".getBytes(StandardCharsets.ISO_8859_1));
    }

    @Test
    void shouldEncodeStringUsingUtf8ByDefault() {
        Client client = client();
        byte[] bytes = client.postStringDefault(SPECIAL_TEXT);
        assertThat(bytes).isEqualTo(SPECIAL_TEXT.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldEncodeStringUsingExplicitUtf8() {
        Client client = client();
        byte[] bytes = client.postStringUtf8(SPECIAL_TEXT);
        assertThat(bytes).isEqualTo(SPECIAL_TEXT.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldEncodeCharArrayUsingIso88591() {
        Client client = client();
        byte[] bytes = client.postCharArrayIso("héllo".toCharArray());
        assertThat(bytes).isEqualTo("héllo".getBytes(StandardCharsets.ISO_8859_1));
    }

    @Test
    void shouldEncodeCharArrayUsingUtf8ByDefault() {
        Client client = client();
        byte[] bytes = client.postCharArrayDefault(SPECIAL_TEXT.toCharArray());
        assertThat(bytes).isEqualTo(SPECIAL_TEXT.getBytes(StandardCharsets.UTF_8));
    }

    @Path("/test-charset")
    public interface Client {
        @POST
        @Path("/string-iso")
        @Consumes("text/plain;charset=ISO-8859-1")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        byte[] postStringIso(String body);

        @POST
        @Path("/string-default")
        @Consumes("text/plain")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        byte[] postStringDefault(String body);

        @POST
        @Path("/string-utf8")
        @Consumes("text/plain;charset=UTF-8")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        byte[] postStringUtf8(String body);

        @POST
        @Path("/chars-iso")
        @Consumes("text/plain;charset=ISO-8859-1")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        byte[] postCharArrayIso(char[] body);

        @POST
        @Path("/chars-default")
        @Consumes("text/plain")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        byte[] postCharArrayDefault(char[] body);
    }

    @Path("/test-charset")
    public static class Resource {
        @POST
        @Path("/string-iso")
        @Consumes("text/plain;charset=ISO-8859-1")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public byte[] postStringIso(@HeaderParam("Content-Type") String contentType, byte[] body) {
            assertThat(contentType).containsIgnoringCase("charset=ISO-8859-1");
            return body;
        }

        @POST
        @Path("/string-default")
        @Consumes("text/plain")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public byte[] postStringDefault(byte[] body) {
            return body;
        }

        @POST
        @Path("/string-utf8")
        @Consumes("text/plain;charset=UTF-8")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public byte[] postStringUtf8(@HeaderParam("Content-Type") String contentType, byte[] body) {
            assertThat(contentType).containsIgnoringCase("charset=UTF-8");
            return body;
        }

        @POST
        @Path("/chars-iso")
        @Consumes("text/plain;charset=ISO-8859-1")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public byte[] postCharArrayIso(@HeaderParam("Content-Type") String contentType, byte[] body) {
            assertThat(contentType).containsIgnoringCase("charset=ISO-8859-1");
            return body;
        }

        @POST
        @Path("/chars-default")
        @Consumes("text/plain")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public byte[] postCharArrayDefault(byte[] body) {
            return body;
        }
    }
}
