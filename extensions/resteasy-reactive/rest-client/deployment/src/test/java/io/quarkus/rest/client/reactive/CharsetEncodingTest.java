package io.quarkus.rest.client.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class CharsetEncodingTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EchoResource.class, EchoClient.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    public void testStringLatin1Charset() {
        EchoClient client = buildClient();
        String response = client.sendLatin1String("é");
        assertEquals("text/plain;charset=ISO-8859-1 e9", response);
    }

    @Test
    public void testStringWindows1252Charset() {
        EchoClient client = buildClient();
        String response = client.sendWindows1252String("€");
        assertEquals("text/plain;charset=windows-1252 80", response);
    }

    @Test
    public void testStringUtf8Charset() {
        EchoClient client = buildClient();
        String response = client.sendUtf8String("é");
        assertEquals("text/plain;charset=UTF-8 c3a9", response);
    }

    @Test
    public void testStringDefaultCharset() {
        EchoClient client = buildClient();
        String response = client.sendDefaultString("é");
        assertEquals("text/plain c3a9", response);
    }

    @Test
    public void testStringUsAsciiCharset() {
        EchoClient client = buildClient();
        String response = client.sendUsAsciiString("test");
        assertEquals("text/plain;charset=US-ASCII 74657374", response);
    }

    @Test
    public void testCharArrayLatin1Charset() {
        EchoClient client = buildClient();
        String response = client.sendLatin1CharArray("é".toCharArray());
        assertEquals("text/plain;charset=ISO-8859-1 e9", response);
    }

    @Test
    public void testCharArrayUtf8Charset() {
        EchoClient client = buildClient();
        String response = client.sendUtf8CharArray("é".toCharArray());
        assertEquals("text/plain;charset=UTF-8 c3a9", response);
    }

    @Test
    public void testStringLatin1MultipleCharacters() {
        EchoClient client = buildClient();
        String response = client.sendLatin1String("café");
        assertEquals("text/plain;charset=ISO-8859-1 636166e9", response);
    }

    @Test
    public void testStringAsciiInLatin1() {
        EchoClient client = buildClient();
        String response = client.sendLatin1String("test");
        assertEquals("text/plain;charset=ISO-8859-1 74657374", response);
    }

    @Test
    public void testEmptyStringWithCharset() {
        EchoClient client = buildClient();
        String response = client.sendLatin1String("");
        assertEquals("text/plain;charset=ISO-8859-1 ", response);
    }

    @Test
    public void testEmptyCharArrayWithCharset() {
        EchoClient client = buildClient();
        String response = client.sendLatin1CharArray(new char[0]);
        assertEquals("text/plain;charset=ISO-8859-1 ", response);
    }

    @Test
    public void testCharsetNameCaseInsensitive() {
        EchoClient client = buildClient();
        String response = client.sendLowercaseUtf8String("é");
        assertEquals("text/plain;charset=utf-8 c3a9", response);
    }

    @Test
    public void testMixedAsciiNonAscii() {
        EchoClient client = buildClient();
        String response = client.sendLatin1String("Helloé");
        assertEquals("text/plain;charset=ISO-8859-1 48656c6c6fe9", response);
    }

    @Test
    public void testLatin1HighRangeCharacter() {
        EchoClient client = buildClient();
        String response = client.sendLatin1String("ÿ");
        assertEquals("text/plain;charset=ISO-8859-1 ff", response);
    }

    @Test
    public void testMultipleNonAsciiCharacters() {
        EchoClient client = buildClient();
        String response = client.sendLatin1String("àéïöü");
        assertEquals("text/plain;charset=ISO-8859-1 e0e9eff6fc", response);
    }

    private EchoClient buildClient() {
        return RestClientBuilder.newBuilder()
                .baseUri(baseUri)
                .build(EchoClient.class);
    }

    @Path("/")
    public static class EchoResource {

        @POST
        @Path("/echo")
        public String echo(byte[] body, @Context HttpHeaders headers) {
            String contentType = headers.getHeaderString("Content-Type");
            if (contentType == null) {
                contentType = "text/plain";
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : body) {
                hex.append(String.format("%02x", b & 0xFF));
            }
            return contentType + " " + hex.toString();
        }
    }

    @Path("/")
    public interface EchoClient {

        @POST
        @Path("/echo")
        @Consumes("text/plain; charset=ISO-8859-1")
        String sendLatin1String(String body);

        @POST
        @Path("/echo")
        @Consumes("text/plain; charset=windows-1252")
        String sendWindows1252String(String body);

        @POST
        @Path("/echo")
        @Consumes("text/plain; charset=UTF-8")
        String sendUtf8String(String body);

        @POST
        @Path("/echo")
        @Consumes("text/plain; charset=utf-8")
        String sendLowercaseUtf8String(String body);

        @POST
        @Path("/echo")
        @Consumes("text/plain; charset=US-ASCII")
        String sendUsAsciiString(String body);

        @POST
        @Path("/echo")
        @Consumes("text/plain")
        String sendDefaultString(String body);

        @POST
        @Path("/echo")
        @Consumes("text/plain; charset=ISO-8859-1")
        String sendLatin1CharArray(char[] body);

        @POST
        @Path("/echo")
        @Consumes("text/plain; charset=UTF-8")
        String sendUtf8CharArray(char[] body);
    }
}
