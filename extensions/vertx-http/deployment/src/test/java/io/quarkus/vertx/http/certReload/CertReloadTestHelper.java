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
        var client = vertx.createHttpClient(options);
        try {
            return client.request(HttpMethod.GET, path)
                    .flatMap(HttpClientRequest::send)
                    .flatMap(HttpClientResponse::body)
                    .map(Buffer::toString)
                    .await();
        } finally {
            client.close();
        }
    }

    static void assertTlsFails(Vertx vertx, HttpClientOptions options, String path) {
        var client = vertx.createHttpClient(options);
        try {
            assertThatThrownBy(() -> client.request(HttpMethod.GET, path)
                    .flatMap(HttpClientRequest::send)
                    .flatMap(HttpClientResponse::body)
                    .map(Buffer::toString)
                    .await()).hasCauseInstanceOf(SSLHandshakeException.class);
        } finally {
            client.close();
        }
    }
}
