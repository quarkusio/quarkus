package io.quarkus.websockets.next.test.errors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.BinaryDecodeException;
import io.quarkus.websockets.next.BinaryEncodeException;
import io.quarkus.websockets.next.OnBinaryMessage;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

public class RuntimeErrorTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Echo.class, RequestBean.class, WSClient.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("echo")
    URI testUri;

    @Test
    void testError() throws InterruptedException {
        WSClient client = WSClient.create(vertx).connect(testUri);
        client.send(Buffer.buffer("1"));
        client.waitForMessages(1);
        assertEquals("Something went wrong", client.getLastMessage().toString());
        assertTrue(RequestBean.DESTROYED_LATCH.await(5, TimeUnit.SECONDS));
    }

    @WebSocket(path = "/echo")
    public static class Echo {

        @Inject
        WebSocketConnection connection;

        @Inject
        RequestBean requestBean;

        @OnBinaryMessage
        void process(WebSocketConnection connection, Buffer message) {
            requestBean.setState("ok");
            throw new IllegalStateException("Something went wrong");
        }

        @OnError
        String encodingError(BinaryEncodeException e) {
            return "Problem encoding: " + e.getEncodedObject().toString();
        }

        @OnError
        String decodingError(BinaryDecodeException e) {
            return "Problem decoding: " + e.getBytes().toString();
        }

        @OnError
        Uni<Void> runtimeProblem(RuntimeException e, WebSocketConnection connection) {
            assertTrue(Context.isOnEventLoopThread());
            assertEquals(connection.id(), this.connection.id());
            // A new request context is used
            assertEquals("nok", requestBean.getState());
            return connection.sendText(e.getMessage());
        }

        @OnError
        String catchAll(Throwable e) {
            return "Ooops!";
        }

    }

    @RequestScoped
    public static class RequestBean {

        static final CountDownLatch DESTROYED_LATCH = new CountDownLatch(1);

        private volatile String state = "nok";

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        @PreDestroy
        void destroy() {
            DESTROYED_LATCH.countDown();
        }

    }

}
