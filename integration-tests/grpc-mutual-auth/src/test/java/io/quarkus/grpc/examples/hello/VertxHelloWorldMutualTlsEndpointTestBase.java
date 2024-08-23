package io.quarkus.grpc.examples.hello;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

abstract class VertxHelloWorldMutualTlsEndpointTestBase {

    abstract Vertx vertx();

    void close(Vertx vertx) {
    }

    protected WebClient create(Vertx vertx) {
        WebClientOptions options = new WebClientOptions();
        options.setSsl(true);
        options.setUseAlpn(true);
        options.setTrustOptions(new PemTrustOptions().addCertValue(buffer("tls/ca.pem")));
        options.setKeyCertOptions(
                new PemKeyCertOptions()
                        .setKeyValue(buffer("tls/client.key"))
                        .setCertValue(buffer("tls/client.pem")));
        return WebClient.create(vertx, options);
    }

    private Buffer buffer(String resource) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            return Buffer.buffer(stream.readAllBytes());
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    @Test
    public void testHelloWorldServiceUsingBlockingStub() throws Exception {
        Vertx vertx = vertx();
        try {
            WebClient client = create(vertx);
            HttpRequest<Buffer> request = client.get(8444, "localhost", "/hello/blocking/neo");
            Future<HttpResponse<Buffer>> fr = request.send();
            String response = fr.toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS).bodyAsString();
            assertThat(response).isEqualTo("Hello neo");
        } finally {
            close(vertx);
        }
    }

    @Test
    public void testHelloWorldServiceUsingMutinyStub() throws Exception {
        Vertx vertx = vertx();
        try {
            WebClient client = create(vertx);
            HttpRequest<Buffer> request = client.get(8444, "localhost", "/hello/mutiny/neo-mutiny");
            Future<HttpResponse<Buffer>> fr = request.send();
            String response = fr.toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS).bodyAsString();
            assertThat(response).isEqualTo("Hello neo-mutiny");
        } finally {
            close(vertx);
        }
    }

}
