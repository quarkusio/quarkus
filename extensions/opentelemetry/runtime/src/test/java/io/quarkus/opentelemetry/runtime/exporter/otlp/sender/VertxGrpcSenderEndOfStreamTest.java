package io.quarkus.opentelemetry.runtime.exporter.otlp.sender;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.opentelemetry.sdk.common.export.GrpcResponse;
import io.opentelemetry.sdk.common.export.GrpcStatusCode;
import io.opentelemetry.sdk.common.export.MessageWriter;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientRequest;
import io.vertx.grpc.client.GrpcClientResponse;

class VertxGrpcSenderEndOfStreamTest {

    private final Vertx vertx = Vertx.vertx();

    @AfterEach
    void closeVertx() throws Exception {
        vertx.close().toCompletionStage().toCompletableFuture().get(10, SECONDS);
    }

    /**
     * When the request future is already complete, the sender sends from the calling thread. If that thread is paused
     * between ending the request and registering its response handlers, the event loop can receive the whole response
     * first. Vert.x does not replay an end of stream to a handler set later, so the export must not depend on it.
     */
    @Test
    void exportCompletesWhenResponseEndsBeforeSenderContinues() throws Exception {
        HttpServer collector = vertx.createHttpServer()
                .requestHandler(request -> request.endHandler(v -> grpcOk(request.response())));
        collector.listen(0, "localhost").toCompletionStage().toCompletableFuture().get(10, SECONDS);

        VertxGrpcSender sender;
        try (MockedStatic<GrpcClient> factory = mockStatic(GrpcClient.class, invocation -> {
            Object result = invocation.callRealMethod();
            return result instanceof GrpcClient client && !Proxy.isProxyClass(client.getClass())
                    ? requestsReadyAtOnce(client)
                    : result;
        })) {
            sender = new VertxGrpcSender(URI.create("http://localhost:" + collector.actualPort()),
                    VertxGrpcSender.GRPC_TRACE_SERVICE_NAME, false, Duration.ofSeconds(1), Map.of(), options -> {
                    }, vertx);
        }

        CompletableFuture<GrpcResponse> result = new CompletableFuture<>();
        sender.send(new EmptyMessageWriter(), result::complete, result::completeExceptionally);

        try {
            assertThat(result.get(5, SECONDS).getStatusCode()).isEqualTo(GrpcStatusCode.OK);
        } finally {
            sender.shutdown();
        }
    }

    // A completed request future makes the sender send from the calling thread, as it does in production
    @SuppressWarnings("unchecked")
    private static GrpcClient requestsReadyAtOnce(GrpcClient client) {
        return delegate(GrpcClient.class, (proxy, method, args) -> {
            Object result = method.invoke(client, args);
            if (method.getName().equals("request") && args != null && args.length == 1
                    && args[0] instanceof SocketAddress) {
                GrpcClientRequest<?, ?> request = ((Future<GrpcClientRequest<?, ?>>) result).toCompletionStage()
                        .toCompletableFuture().get(5, SECONDS);
                return Future.succeededFuture(pausedUntilResponseEnds(request));
            }
            return result;
        });
    }

    // Holds the sender thread after the request is sent until the response has ended
    private static GrpcClientRequest<?, ?> pausedUntilResponseEnds(GrpcClientRequest<?, ?> request) {
        AtomicBoolean ended = new AtomicBoolean();
        return delegate(GrpcClientRequest.class, (proxy, method, args) -> {
            if (method.getName().equals("end") || method.getName().equals("send")) {
                ended.set(true);
            }
            if (method.getName().equals("response") && ended.get() && !Context.isOnEventLoopThread()) {
                GrpcClientResponse<?, ?> response = request.response().toCompletionStage().toCompletableFuture()
                        .get(5, SECONDS);
                response.end().toCompletionStage().toCompletableFuture().handle((v, t) -> v).get(5, SECONDS);
            }
            return method.isDefault() ? InvocationHandler.invokeDefault(proxy, method, args) : method.invoke(request, args);
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T delegate(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
            try {
                return handler.invoke(proxy, method, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }

    private static void grpcOk(HttpServerResponse response) {
        response.putHeader("content-type", "application/grpc");
        // one empty message: no compression flag, zero length
        response.write(Buffer.buffer(new byte[5]));
        response.putTrailer("grpc-status", "0");
        response.end();
    }

    private static final class EmptyMessageWriter implements MessageWriter {
        @Override
        public void writeMessage(OutputStream output) {
        }

        @Override
        public int getContentLength() {
            return 0;
        }
    }
}
