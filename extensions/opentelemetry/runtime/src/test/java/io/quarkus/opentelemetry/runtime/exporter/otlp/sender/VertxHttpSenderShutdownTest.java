package io.quarkus.opentelemetry.runtime.exporter.otlp.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientAgent;
import io.vertx.core.http.HttpClientBuilder;
import io.vertx.core.http.HttpClientOptions;

class VertxHttpSenderShutdownTest {

    private VertxHttpSender createSender(HttpClientAgent client) {
        Vertx vertx = mock(Vertx.class);
        HttpClientBuilder builder = mock(HttpClientBuilder.class);
        when(vertx.httpClientBuilder()).thenReturn(builder);
        when(builder.with(any(HttpClientOptions.class))).thenReturn(builder);
        when(builder.withConnectHandler(any())).thenReturn(builder);
        when(builder.build()).thenReturn(client);
        return new VertxHttpSender(URI.create("http://localhost:4318"), "/v1/traces", false, Duration.ofSeconds(10),
                Map.of(), "application/x-protobuf", options -> {
                }, vertx);
    }

    @Test
    void shutdownSucceedsWhenCloseReturnsNull() {
        // client.close() can return null when the underlying client was already reclaimed,
        // see https://github.com/eclipse-vertx/vert.x/issues/6302
        HttpClientAgent client = mock(HttpClientAgent.class);
        when(client.close()).thenReturn(null);

        var result = createSender(client).shutdown();

        assertThat(result.isDone()).isTrue();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shutdownSucceedsWhenCloseSucceeds() {
        HttpClientAgent client = mock(HttpClientAgent.class);
        when(client.close()).thenReturn(Future.succeededFuture());

        var result = createSender(client).shutdown();

        assertThat(result.isDone()).isTrue();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shutdownFailsWhenCloseFails() {
        HttpClientAgent client = mock(HttpClientAgent.class);
        when(client.close()).thenReturn(Future.failedFuture(new IllegalStateException()));

        var result = createSender(client).shutdown();

        assertThat(result.isDone()).isTrue();
        assertThat(result.isSuccess()).isFalse();
    }
}
