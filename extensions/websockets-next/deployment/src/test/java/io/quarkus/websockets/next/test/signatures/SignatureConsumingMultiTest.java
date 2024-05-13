package io.quarkus.websockets.next.test.signatures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.net.URI;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

public class SignatureConsumingMultiTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(BiDirectional.class, WSClient.class, RequestScopedBean.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("/")
    URI uri;

    @Test
    void verifyExecutionOfOnMessageWhenConsumingAndReturningMultis() {
        WSClient client = WSClient.create(vertx).connect(WSClient.toWS(uri, "/ws/%s/%d".formatted("bi-directional", 3)));

        for (int i = 0; i < 10; i++) {
            client.sendAndAwait("hello" + i);
        }

        await().until(() -> client.getMessages().size() == 10);
        assertThat(client.getMessages().stream().map(Buffer::toString).collect(Collectors.toList()))
                .containsExactlyInAnyOrderElementsOf(
                        IntStream.range(0, 10).mapToObj(id -> "WS " + 3 + " received: hello" + id)
                                .collect(Collectors.toList()));
    }

    @WebSocket(path = "/ws/bi-directional/{id}")
    public static class BiDirectional {

        @Inject
        WebSocketConnection connection;

        @Inject
        RequestScopedBean requestScopedBean;

        volatile Context context = null;

        @OnTextMessage
        Multi<String> process(Multi<String> multi) {
            assertThat(Context.isOnEventLoopThread()).isTrue();
            Context context = Vertx.currentContext();
            assertThat(context).isNotNull();
            assertThat(VertxContext.isOnDuplicatedContext()).isTrue();
            String requestScopedBeanId = requestScopedBean.getId();

            assertThat(connection).isNotNull();
            if (this.context == null) {
                this.context = context;
            }

            return multi.map(s -> {
                assertThat(this.context).isSameAs(Vertx.currentContext());
                // For bi-directional streams we subscribe to the returned Multi during onOpen
                // As a result the same duplicated context is used but a new request context is activated/terminated per each message processing
                assertNotEquals(requestScopedBeanId, requestScopedBean.getId());
                String id = connection.pathParam("id");
                return "WS " + id + " received: " + s;
            });
        }

    }

    @RequestScoped
    public static class RequestScopedBean {

        private String id;

        @PostConstruct
        void init() {
            id = UUID.randomUUID().toString();
        }

        String getId() {
            return id;
        }

    }

}
