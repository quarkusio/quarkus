package io.quarkus.opentelemetry.runtime.exporter.otlp.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.grpc.client.GrpcClient;

class VertxGrpcSenderShutdownTest {

    private VertxGrpcSender createSender(GrpcClient client) {
        try (MockedStatic<GrpcClient> grpcClientFactory = mockStatic(GrpcClient.class)) {
            grpcClientFactory.when(() -> GrpcClient.client(any(Vertx.class), any(HttpClientOptions.class)))
                    .thenReturn(client);
            return new VertxGrpcSender(URI.create("http://localhost:4317"), VertxGrpcSender.GRPC_TRACE_SERVICE_NAME, false,
                    Duration.ofSeconds(10), Map.of(), options -> {
                    }, mock(Vertx.class));
        }
    }

    @Test
    void shutdownSucceedsWhenCloseReturnsNull() {
        // client.close() can return null when the underlying client was already reclaimed,
        // see https://github.com/eclipse-vertx/vert.x/issues/6302
        GrpcClient client = mock(GrpcClient.class);
        when(client.close()).thenReturn(null);

        var result = createSender(client).shutdown();

        assertThat(result.isDone()).isTrue();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void shutdownSucceedsWhenCloseSucceeds() {
        GrpcClient client = mock(GrpcClient.class);
        when(client.close()).thenReturn(Future.succeededFuture());

        var result = createSender(client).shutdown();

        assertThat(result.isDone()).isTrue();
        assertThat(result.isSuccess()).isTrue();
    }
}
