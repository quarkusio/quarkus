package io.quarkus.it.rest.client.http2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@QuarkusTest
public class ResourceTest {
    @TestHTTPResource(value = "/ping")
    URL sslUrl;

    @TestHTTPResource(value = "/client/ping")
    URL clientUrl;

    @TestHTTPResource(value = "/client2/ping")
    URL client2Url;

    private WebClient webClient;

    @BeforeEach
    public void setup() {
        webClient = createWebClient();
    }

    @AfterEach
    public void tearDown() {
        if (webClient != null) {
            webClient.close();
        }
    }

    @Test
    public void shouldReturnPongFromServer() throws Exception {
        HttpResponse<?> response = call(sslUrl);
        assertEquals("pong", response.bodyAsString());
        assertEquals("HTTP_2", response.version().name());
    }

    @Test
    public void shouldReturnPongFromClient() throws Exception {
        HttpResponse<?> response = call(clientUrl);
        // if it's empty, it's because the REST Client is not using the HTTP/2 version
        assertEquals("pong", response.bodyAsString());
    }

    @Test
    public void shouldReturnPongFromManuallyCreatedClient() throws Exception {
        HttpResponse<?> response = call(client2Url);
        // if it's empty, it's because the REST Client is not using the HTTP/2 version
        assertEquals("pong", response.bodyAsString());
    }

    private HttpResponse<?> call(URL url) throws Exception {
        CompletableFuture<HttpResponse<Buffer>> result = new CompletableFuture<>();
        webClient.get(url.getPort(), url.getHost(), url.getPath())
                .send(ar -> {
                    if (ar.succeeded()) {
                        result.complete(ar.result());
                    } else {
                        result.completeExceptionally(ar.cause());
                    }
                });

        return result.get();
    }

    private WebClient createWebClient() {
        Vertx vertx = Vertx.vertx();
        WebClientOptions options = new WebClientOptions()
                .setUseAlpn(true)
                .setProtocolVersion(HttpVersion.HTTP_2);

        return WebClient.create(vertx, options);
    }
}
