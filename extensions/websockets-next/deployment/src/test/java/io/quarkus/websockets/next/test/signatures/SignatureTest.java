package io.quarkus.websockets.next.test.signatures;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

public class SignatureTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(MethodReturningString.class, UniWs.class, MultiWs.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("/")
    URI uri;

    private static Stream<Arguments> methods() {
        return Stream.of(
                Arguments.of("string", 1),
                Arguments.of("uni", 2),
                Arguments.of("multi", 3));
    }

    @ParameterizedTest(name = "{index} Checking the reception of message for method returning {0}")
    @MethodSource("methods")
    void verifyExecutionOfOnMessage(String path, int id) {
        WSClient client = WSClient.create(vertx).connect(WSClient.toWS(uri, "/ws/%s/%d".formatted(path, id)));
        Buffer resp = client.sendAndAwaitReply("hello");
        assertThat(resp.toString()).isEqualTo("WS " + id + " received: hello");
    }

    @WebSocket(path = "/ws/string/{id}")
    public static class MethodReturningString {

        @Inject
        WebSocketConnection connection;

        @OnTextMessage
        String process(String message) {
            assertThat(Context.isOnEventLoopThread()).isFalse();
            assertThat(Vertx.currentContext()).isNotNull();
            assertThat(VertxContext.isOnDuplicatedContext()).isTrue();

            assertThat(connection).isNotNull();

            String id = connection.pathParam("id");
            return "WS " + id + " received: " + message;
        }

    }

    @WebSocket(path = "/ws/uni/{id}")
    public static class UniWs {

        @Inject
        WebSocketConnection connection;

        @OnTextMessage
        Uni<String> process(String message) {
            assertThat(Context.isOnEventLoopThread()).isTrue();
            Context context = Vertx.currentContext();
            assertThat(context).isNotNull();
            assertThat(VertxContext.isOnDuplicatedContext()).isTrue();

            assertThat(connection).isNotNull();

            return Uni.createFrom().item(() -> {
                assertThat(context).isSameAs(Vertx.currentContext());
                String id = connection.pathParam("id");
                return "WS " + id + " received: " + message;
            }).emitOn(Infrastructure.getDefaultExecutor());
        }
    }

    @WebSocket(path = "/ws/multi/{id}")
    public static class MultiWs {

        @Inject
        WebSocketConnection connection;

        @OnTextMessage
        Multi<String> process(String message) {
            assertThat(Context.isOnEventLoopThread()).isTrue();
            Context context = Vertx.currentContext();
            assertThat(context).isNotNull();
            assertThat(VertxContext.isOnDuplicatedContext()).isTrue();

            assertThat(connection).isNotNull();

            return Multi.createFrom().item(() -> {
                assertThat(context).isSameAs(Vertx.currentContext());
                String id = connection.pathParam("id");
                return "WS " + id + " received: " + message;
            }).emitOn(Infrastructure.getDefaultExecutor());
        }
    }
}
