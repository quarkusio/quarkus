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

/**
 * Tests that REST Client properly respects the charset parameter in the Content-Type header
 * when serializing String and char[] request bodies. Verifies that StringMessageBodyHandler
 * and CharArrayMessageBodyHandler use MessageReaderUtil.charsetFromMediaType() instead of
 * hardcoding UTF-8 encoding.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/57627">Issue 57627</a>
 */
public class CharsetEncodingTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(EchoResource.class, EchoClient.class));

    @TestHTTPResource
    URI baseUri;

    /**
     * Verifies ISO-8859-1 encoding for single non-ASCII character.
     * Character 'é' is 0xE9 in ISO-8859-1 but 0xC3A9 in UTF-8.
     */
    @Test
    public void testStringLatin1Charset() {
        EchoClient client = buildClient();
        String response = client.sendLatin1String("é");
        assertEquals("text/plain;charset=ISO-8859-1 e9", response);
    }

    /**
     * Verifies Windows-1252 encoding for Euro sign.
     * Character '€' is 0x80 in Windows-1252 but 0xE282AC in UTF-8.
     */
    @Test
    public void testStringWindows1252Charset() {
        EchoClient client = buildClient();
        String response = client.sendWindows1252String("€");
        assertEquals("text/plain;charset=windows-1252 80", response);
    }

    /**
     * Verifies explicit UTF-8 encoding when specified in Content-Type.
     */
    @Test
    public void testStringUtf8Charset() {
        EchoClient client = buildClient();
        String response = client.sendUtf8String("é");
        assertEquals("text/plain;charset=UTF-8 c3a9", response);
    }

    /**
     * Verifies default UTF-8 encoding when no charset parameter is specified.
     * This ensures backward compatibility with existing code.
     */
    @Test
    public void testStringDefaultCharset() {
        EchoClient client = buildClient();
        String response = client.sendDefaultString("é");
        assertEquals("text/plain c3a9", response);
    }

    /**
     * Verifies US-ASCII encoding for ASCII-range characters.
     * All ASCII characters (0x00-0x7F) encode identically in US-ASCII and UTF-8.
     */
    @Test
    public void testStringUsAsciiCharset() {
        EchoClient client = buildClient();
        String response = client.sendUsAsciiString("test");
        assertEquals("text/plain;charset=US-ASCII 74657374", response);
    }

    /**
     * Verifies char[] encoding with ISO-8859-1 charset.
     * Ensures CharArrayMessageBodyHandler respects charset parameter.
     */
    @Test
    public void testCharArrayLatin1Charset() {
        EchoClient client = buildClient();
        String response = client.sendLatin1CharArray("é".toCharArray());
        assertEquals("text/plain;charset=ISO-8859-1 e9", response);
    }

    /**
     * Verifies char[] encoding with UTF-8 charset.
     */
    @Test
    public void testCharArrayUtf8Charset() {
        EchoClient client = buildClient();
        String response = client.sendUtf8CharArray("é".toCharArray());
        assertEquals("text/plain;charset=UTF-8 c3a9", response);
    }

    /**
     * Verifies multi-character string encoding with ISO-8859-1.
     * Tests: 'c'=0x63, 'a'=0x61, 'f'=0x66, 'é'=0xE9 in ISO-8859-1.
     */
    @Test
    public void testStringLatin1MultipleCharacters() {
        EchoClient client = buildClient();
        String response = client.sendLatin1String("café");
        assertEquals("text/plain;charset=ISO-8859-1 636166e9", response);
    }

    /**
     * Verifies that ASCII characters encode identically regardless of charset.
     * ASCII subset (0x00-0x7F) is identical across UTF-8, ISO-8859-1, Windows-1252, etc.
     */
    @Test
    public void testStringAsciiInLatin1() {
        EchoClient client = buildClient();
        String response = client.sendLatin1String("test");
        assertEquals("text/plain;charset=ISO-8859-1 74657374", response);
    }

    /**
     * Verifies empty string handling with explicit charset.
     * Empty body should result in empty hex string.
     */
    @Test
    public void testEmptyStringWithCharset() {
        EchoClient client = buildClient();
        String response = client.sendLatin1String("");
        assertEquals("text/plain;charset=ISO-8859-1 ", response);
    }

    /**
     * Verifies empty char array handling with explicit charset.
     */
    @Test
    public void testEmptyCharArrayWithCharset() {
        EchoClient client = buildClient();
        String response = client.sendLatin1CharArray(new char[0]);
        assertEquals("text/plain;charset=ISO-8859-1 ", response);
    }

    /**
     * Verifies charset name is case-insensitive per RFC 2045.
     * Tests lowercase variant of charset name.
     */
    @Test
    public void testCharsetNameCaseInsensitive() {
        EchoClient client = buildClient();
        String response = client.sendLowercaseUtf8String("é");
        assertEquals("text/plain;charset=utf-8 c3a9", response);
    }

    /**
     * Verifies mixed ASCII and non-ASCII content with ISO-8859-1.
     * Tests: 'H'=0x48, 'e'=0x65, 'l'=0x6C, 'l'=0x6C, 'o'=0x6F, 'é'=0xE9.
     */
    @Test
    public void testMixedAsciiNonAscii() {
        EchoClient client = buildClient();
        String response = client.sendLatin1String("Helloé");
        assertEquals("text/plain;charset=ISO-8859-1 48656c6c6fe9", response);
    }

    /**
     * Verifies high-range ISO-8859-1 character.
     * Character 'ÿ' (Latin small letter y with diaeresis) is 0xFF in ISO-8859-1,
     * the highest single-byte character in the charset.
     */
    @Test
    public void testLatin1HighRangeCharacter() {
        EchoClient client = buildClient();
        String response = client.sendLatin1String("ÿ");
        assertEquals("text/plain;charset=ISO-8859-1 ff", response);
    }

    /**
     * Verifies multiple different non-ASCII characters in single request.
     * Tests various accented characters: à=0xE0, é=0xE9, ï=0xEF, ö=0xF6, ü=0xFC.
     */
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

    /**
     * Server resource that echoes back the Content-Type header and hexadecimal
     * representation of received bytes. This allows verification of the actual
     * byte encoding sent by the client.
     */
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

    /**
     * REST client interface with various charset declarations.
     * Each method tests a different charset encoding scenario.
     */
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
