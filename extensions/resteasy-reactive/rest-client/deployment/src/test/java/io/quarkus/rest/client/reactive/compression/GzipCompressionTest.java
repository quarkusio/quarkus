package io.quarkus.rest.client.reactive.compression;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.rest.client.reactive.TestUtils;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.HttpServerOptionsCustomizer;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

public class GzipCompressionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Endpoint.class, Client1.class, TestUtils.class))
            .overrideRuntimeConfigKey("quarkus.rest-client.client1.url", "http://localhost:${quarkus.http.test-port:8081}")
            .overrideRuntimeConfigKey("quarkus.rest-client.client1.enable-compression", "true")
            .overrideRuntimeConfigKey("quarkus.rest-client.client2.url", "http://localhost:${quarkus.http.test-port:8081}")
            .overrideRuntimeConfigKey("quarkus.rest-client.client2.enable-compression", "false");

    private static final String uncompressedString;
    private static final byte[] uncompressedBytes;

    static {
        uncompressedString = TestUtils.randomAlphaString(1000);
        uncompressedBytes = uncompressedString.getBytes();
    }

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "quarkus.http.test-port", defaultValue = "8081")
    Integer port;

    @RestClient
    Client1 client1;

    @RestClient
    Client2 client2;

    /**
     * This test is very important to ensure that we know the server is actually capable of sending
     * gzip encoded data
     */
    @Test
    void ensureServerCanSendCompressedData()
            throws ExecutionException, InterruptedException, TimeoutException, IOException {
        CompletableFuture<Buffer> receivedBufferCF = new CompletableFuture<>();
        WebClient client = WebClient.create(vertx);
        try {
            client.get(port, "localhost", "/client/message")
                    .putHeader(HttpHeaderNames.ACCEPT_ENCODING.toString(), "gzip")
                    .putHeader(HttpHeaderNames.ACCEPT.toString(), "text/plain")
                    .as(BodyCodec.buffer())
                    .send()
                    .onFailure(receivedBufferCF::completeExceptionally)
                    .onSuccess(response -> {
                        receivedBufferCF.complete(response.bodyAsBuffer());
                    });
            Buffer receivedBuffer = receivedBufferCF.get(10, TimeUnit.SECONDS);
            byte[] receivedBytes = receivedBuffer.getBytes();
            assertThat(receivedBytes).isNotEqualTo(uncompressedBytes);
            assertThat(TestUtils.decompressGzip(receivedBytes)).isEqualTo(uncompressedBytes);
        } finally {
            client.close();
        }
    }

    /**
     * We need to know that the server can send uncompressed data as well
     */
    @Test
    void ensureServerCanSendUncompressedData() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Buffer> receivedBufferCF = new CompletableFuture<>();
        WebClient client = WebClient.create(vertx);
        try {
            client.get(port, "localhost", "/client/message")
                    .putHeader(HttpHeaderNames.ACCEPT.toString(), "text/plain")
                    .as(BodyCodec.buffer())
                    .send()
                    .onFailure(receivedBufferCF::completeExceptionally)
                    .onSuccess(response -> {
                        receivedBufferCF.complete(response.bodyAsBuffer());
                    });
            Buffer receivedBuffer = receivedBufferCF.get(10, TimeUnit.SECONDS);
            assertThat(receivedBuffer.getBytes()).isEqualTo(uncompressedBytes);
        } finally {
            client.close();
        }
    }

    // now we can actually test the client as we know the server behaves as expected

    @Test
    void testReceiveCompressed() {
        assertThat(client1.receiveCompressed()).isEqualTo(uncompressedString);
    }

    @Test
    void testReceiveUncompressed() {
        assertThat(client1.receiveCompressed()).isEqualTo(uncompressedString);
    }

    @Test
    void testReceiveCompressedInClient2() throws IOException {
        byte[] receivedBytes = client2.receiveCompressed();
        assertThat(receivedBytes).isNotEqualTo(uncompressedBytes);
        assertThat(TestUtils.decompressGzip(receivedBytes)).isEqualTo(uncompressedBytes);
    }

    /**
     * This ensures that Vert.x will automatically compress the body of an HTTP response when the Accept-Encoding HTTP
     * header requests indicates the client supports such compression
     */
    @Singleton
    public static class ServerOptionsCustomizer implements HttpServerOptionsCustomizer {

        @Override
        public void customizeHttpServer(HttpServerOptions options) {
            options.setCompressionSupported(true);
        }
    }

    /**
     * We don't use Quarkus REST here as we want to make sure that don't involve any other layer that could potentially
     * be adding compression. This way the compression will be provided only by Vert.x
     */
    public static class Endpoint {

        public void setup(@Observes Router router) {
            router.route("/client/message").handler(new Handler<>() {
                @Override
                public void handle(RoutingContext rc) {
                    HttpServerRequest req = rc.request();
                    HttpServerResponse response = req.response().setStatusCode(200).putHeader(HttpHeaderNames.CONTENT_TYPE,
                            "text/plain");
                    Buffer body = Buffer.buffer(uncompressedBytes);
                    response.end(body);
                }
            });
        }
    }

    /**
     * This client has {@code enable-compression} set to {@code true}
     */
    @Path("/client")
    @RegisterRestClient(configKey = "client1")
    public interface Client1 {
        @ClientHeaderParam(name = "Accept-Encoding", value = "gzip")
        @GET
        @Path("/message")
        @Produces(MediaType.TEXT_PLAIN)
        String receiveCompressed();

        @GET
        @Path("/message")
        @Produces(MediaType.TEXT_PLAIN)
        String receiveUncompressed();
    }

    /**
     * This client has {@code enable-compression} set to {@code false}
     */
    @Path("/client")
    @RegisterRestClient(configKey = "client2")
    public interface Client2 {
        @ClientHeaderParam(name = "Accept-Encoding", value = "gzip")
        @GET
        @Path("/message")
        @Produces(MediaType.TEXT_PLAIN)
        byte[] receiveCompressed();
    }
}
