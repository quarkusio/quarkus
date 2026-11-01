package io.quarkus.rest.client.reactive.compression;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.rest.client.reactive.TestUtils;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.HttpServerConfigCustomizer;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.CompressionConfig;
import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

/**
 * A gzip response larger than a single network buffer can be consumed as an {@code InputStream}: the decompression
 * happens in the transport, so nothing reads the stream on the event loop.
 */
public class GzipInputStreamCompressionTest {

    private static final int CHUNK_SIZE = 64 * 1024;
    private static final int CHUNKS = 32;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Endpoint.class, ServerOptionsCustomizer.class, ExplicitClient.class, FallbackClient.class,
                            RawClient.class,
                            TestUtils.class)
                    .addAsResource(new StringAsset("quarkus.http.enable-compression=true\n"), "application.properties"))
            .overrideRuntimeConfigKey("quarkus.rest-client.explicit.url", "http://localhost:${quarkus.http.test-port:8081}")
            .overrideRuntimeConfigKey("quarkus.rest-client.explicit.enable-response-decompression", "true")
            .overrideRuntimeConfigKey("quarkus.rest-client.fallback.url", "http://localhost:${quarkus.http.test-port:8081}")
            .overrideRuntimeConfigKey("quarkus.rest-client.raw.url", "http://localhost:${quarkus.http.test-port:8081}")
            .overrideRuntimeConfigKey("quarkus.rest-client.raw.enable-response-decompression", "false");

    private static final String uncompressedString = TestUtils.randomAlphaString(CHUNK_SIZE);

    @RestClient
    ExplicitClient explicitClient;

    @RestClient
    FallbackClient fallbackClient;

    @RestClient
    RawClient rawClient;

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "quarkus.http.test-port", defaultValue = "8081")
    Integer port;

    @Test
    void serverSendsTheLargeBodyCompressed() throws Exception {
        CompletableFuture<Buffer> received = new CompletableFuture<>();
        WebClient client = WebClient.create(vertx);
        try {
            client.get(port, "localhost", "/client/large")
                    .putHeader(HttpHeaderNames.ACCEPT_ENCODING.toString(), "gzip")
                    .as(BodyCodec.buffer())
                    .send()
                    .onFailure(received::completeExceptionally)
                    .onSuccess(response -> received.complete(response.bodyAsBuffer()));
            byte[] bytes = received.get(10, TimeUnit.SECONDS).getBytes();
            assertThat(bytes.length).isLessThan(CHUNK_SIZE * CHUNKS);
            assertThat(new String(TestUtils.decompressGzip(bytes), StandardCharsets.UTF_8))
                    .isEqualTo(uncompressedString.repeat(CHUNKS));
        } finally {
            client.close();
        }
    }

    @Test
    void streamIsCompressedWhenDecompressionIsDisabled() throws IOException {
        try (InputStream in = rawClient.receiveCompressedStream()) {
            byte[] received = in.readAllBytes();
            assertThat(new String(received, StandardCharsets.UTF_8)).isNotEqualTo(uncompressedString.repeat(CHUNKS));
            assertThat(new String(TestUtils.decompressGzip(received), StandardCharsets.UTF_8))
                    .isEqualTo(uncompressedString.repeat(CHUNKS));
        }
    }

    @Test
    void streamIsDecompressedWithExplicitConfiguration() throws IOException {
        try (InputStream in = explicitClient.receiveCompressedStream()) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(uncompressedString.repeat(CHUNKS));
        }
    }

    @Test
    void streamIsDecompressedWithTheGlobalCompressionSetting() throws IOException {
        try (InputStream in = fallbackClient.receiveCompressedStream()) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(uncompressedString.repeat(CHUNKS));
        }
    }

    @Singleton
    public static class ServerOptionsCustomizer implements HttpServerConfigCustomizer {

        @Override
        public void customizeHttpServer(HttpServerConfig config) {
            CompressionConfig compression = new CompressionConfig();
            compression.setCompressionEnabled(true);
            compression.addGzip();
            config.setCompressionConfig(compression);
        }
    }

    public static class Endpoint {

        public void setup(@Observes Router router) {
            router.route("/client/large").handler(rc -> {
                HttpServerResponse response = rc.response().setStatusCode(200)
                        .putHeader(HttpHeaderNames.CONTENT_TYPE, "text/plain")
                        .setChunked(true);
                response.headers().remove(HttpHeaderNames.CONTENT_ENCODING);
                writeChunks(response, CHUNKS);
            });
        }

        private static void writeChunks(HttpServerResponse response, int remaining) {
            if (remaining == 0) {
                response.end();
                return;
            }
            response.write(Buffer.buffer(uncompressedString)).onComplete(ignored -> writeChunks(response, remaining - 1));
        }
    }

    @Path("/client")
    @RegisterRestClient(configKey = "explicit")
    public interface ExplicitClient {
        @ClientHeaderParam(name = "Accept-Encoding", value = "gzip")
        @GET
        @Path("/large")
        @Produces(MediaType.TEXT_PLAIN)
        InputStream receiveCompressedStream();
    }

    @Path("/client")
    @RegisterRestClient(configKey = "raw")
    public interface RawClient {
        @ClientHeaderParam(name = "Accept-Encoding", value = "gzip")
        @GET
        @Path("/large")
        @Produces(MediaType.TEXT_PLAIN)
        InputStream receiveCompressedStream();
    }

    @Path("/client")
    @RegisterRestClient(configKey = "fallback")
    public interface FallbackClient {
        @ClientHeaderParam(name = "Accept-Encoding", value = "gzip")
        @GET
        @Path("/large")
        @Produces(MediaType.TEXT_PLAIN)
        InputStream receiveCompressedStream();
    }
}
