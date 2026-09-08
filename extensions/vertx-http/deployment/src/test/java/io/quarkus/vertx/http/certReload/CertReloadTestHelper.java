package io.quarkus.vertx.http.certReload;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.net.ssl.SSLHandshakeException;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;

final class CertReloadTestHelper {

    private CertReloadTestHelper() {
    }

    static String httpsGet(Vertx vertx, HttpClientOptions options, String path) {
        return vertx.createHttpClient(options)
                .request(HttpMethod.GET, path)
                .flatMap(HttpClientRequest::send)
                .flatMap(HttpClientResponse::body)
                .map(Buffer::toString)
                .toCompletionStage().toCompletableFuture().join();
    }

    static void assertTlsFails(Vertx vertx, HttpClientOptions options, String path) {
        assertThatThrownBy(() -> vertx.createHttpClient(options)
                .request(HttpMethod.GET, path)
                .flatMap(HttpClientRequest::send)
                .flatMap(HttpClientResponse::body)
                .map(Buffer::toString)
                .toCompletionStage().toCompletableFuture().join()).hasCauseInstanceOf(SSLHandshakeException.class);
    }
}
