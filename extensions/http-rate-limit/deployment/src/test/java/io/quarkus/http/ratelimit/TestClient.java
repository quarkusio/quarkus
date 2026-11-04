package io.quarkus.http.ratelimit;

import java.net.URL;
import java.time.Duration;
import java.util.Map;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

/**
 * A web client bound to the application under test, for one test method: create it in {@code @BeforeEach} from the
 * injected {@link Vertx} and {@code @TestHTTPResource} URL, and close it in {@code @AfterEach}. Each request is sent
 * with {@link TestHttp#send}.
 */
final class TestClient {

    private final Vertx vertx;
    private final URL url;
    private final WebClient client;

    TestClient(Vertx vertx, URL url) {
        this.vertx = vertx;
        this.url = url;
        this.client = WebClient.create(vertx);
    }

    HttpResponse<Buffer> get(String path) {
        return get(path, Map.of());
    }

    HttpResponse<Buffer> get(String path, Map<String, String> headers) {
        return TestHttp.get(vertx, client, url, path, headers);
    }

    HttpResponse<Buffer> send(HttpMethod method, String path, Map<String, String> headers, Buffer body) {
        return TestHttp.send(vertx, client, url, method, path, headers, body);
    }

    HttpResponse<Buffer> send(HttpMethod method, String path, Map<String, String> headers, Buffer body,
            Duration timeout) {
        return TestHttp.send(vertx, client, url, method, path, headers, body, timeout);
    }

    void close() {
        client.close();
    }
}
