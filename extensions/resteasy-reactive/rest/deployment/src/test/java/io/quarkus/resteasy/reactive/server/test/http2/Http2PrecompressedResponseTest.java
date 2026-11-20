package io.quarkus.resteasy.reactive.server.test.http2;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

/**
 * A response that already declares {@code Content-Encoding: gzip} must not be compressed a second
 * time by the server compressor, over HTTP/2 as over HTTP/1.1.
 */
public class Http2PrecompressedResponseTest {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final byte[] GZIP_MAGIC = new byte[] { 0x1f, (byte) 0x8b };
    private static final String JSON = "{\"message\":\"hello world\"}";

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(PrecompressedResource.class))
            .overrideConfigKey("quarkus.http.enable-compression", "true");

    @Inject
    Vertx vertx;

    @TestHTTPResource("/precompressed")
    URL url;

    @TestHTTPResource("/precompressed/plain")
    URL plainUrl;

    @Test
    public void http2() throws IOException {
        HttpResponse<Buffer> response = get(url, new WebClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(false));
        assertThat(response.version()).isEqualTo(HttpVersion.HTTP_2);
        assertSingleGzipLayer(response);
    }

    @Test
    public void http11() throws IOException {
        HttpResponse<Buffer> response = get(url, new WebClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_1_1));
        assertThat(response.version()).isEqualTo(HttpVersion.HTTP_1_1);
        assertSingleGzipLayer(response);
    }

    /**
     * The server compressor is applied over HTTP/2, so the single gzip layer asserted above
     * is not just the one produced by the resource itself.
     */
    @Test
    public void http2CompressesRegularResponse() throws IOException {
        HttpResponse<Buffer> response = get(plainUrl, new WebClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(false));
        assertThat(response.version()).isEqualTo(HttpVersion.HTTP_2);
        assertSingleGzipLayer(response);
    }

    private void assertSingleGzipLayer(HttpResponse<Buffer> response) throws IOException {
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.getHeader("content-encoding")).isEqualTo("gzip");
        byte[] compressed = response.body().getBytes();
        assertThat(compressed).startsWith(GZIP_MAGIC);
        // compressing the data a second time would leave a gzip stream inside the first one
        assertThat(gunzip(compressed)).isEqualTo(JSON.getBytes(StandardCharsets.UTF_8));
    }

    private HttpResponse<Buffer> get(URL target, WebClientOptions options) {
        WebClient client = WebClient.create(vertx, options.setDecompressionSupported(false));
        try {
            return client.get(target.getPort(), target.getHost(), target.getPath())
                    .putHeader("accept-encoding", "gzip")
                    .send().await().atMost(REQUEST_TIMEOUT);
        } finally {
            client.close();
        }
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
            out.write(data);
        }
        return bytes.toByteArray();
    }

    private static byte[] gunzip(byte[] data) throws IOException {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data))) {
            return in.readAllBytes();
        }
    }

    @Path("/precompressed")
    public static class PrecompressedResource {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response get() throws IOException {
            return Response.ok(gzip(JSON.getBytes(StandardCharsets.UTF_8)), MediaType.APPLICATION_JSON)
                    .header("content-encoding", "gzip")
                    .build();
        }

        @GET
        @Path("/plain")
        @Produces(MediaType.APPLICATION_JSON)
        public String plain() {
            return JSON;
        }
    }
}
