package io.quarkus.websockets.next.test.requestcontext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

public class RequestContextTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(AppendBlocking.class, WSClient.class, RequestScopedBean.class, RequestContextListener.class);
            })
            // Disable SR Context Propagation for ArC, otherwise there could be a mess in context lifecycle events
            .overrideConfigKey("quarkus.arc.context-propagation.enabled", "false");

    @Inject
    Vertx vertx;

    @TestHTTPResource("append")
    URI appendUri;

    @TestHTTPResource("append-blocking")
    URI appendBlockingUri;

    @Inject
    RequestContextListener listener;

    @Test
    void testRequestContext() throws InterruptedException {
        assertRequestContext(appendUri);
    }

    @Test
    void testRequestContextBlocking() throws InterruptedException {
        assertRequestContext(appendBlockingUri);
    }

    private void assertRequestContext(URI testUri) throws InterruptedException {
        // Remove all events that could be fired due to startup observers
        listener.clear();
        RequestScopedBean.COUNTER.set(0);

        WSClient client = WSClient.create(vertx).connect(testUri);
        client.send("foo");
        client.send("bar");
        client.send("baz");
        client.waitForMessages(3);
        assertEquals("foo:1", client.getMessages().get(0).toString());
        assertEquals("bar:2", client.getMessages().get(1).toString());
        assertEquals("baz:3", client.getMessages().get(2).toString());
        client.disconnect();
        assertTrue(RequestScopedBean.DESTROYED_LATCH.await(5, TimeUnit.SECONDS),
                "Latch count: " + RequestScopedBean.DESTROYED_LATCH.getCount());
        assertEquals(9, listener.events.size());
        assertTrue(listener.events.get(0).qualifiers().contains(Initialized.Literal.REQUEST));
        assertTrue(listener.events.get(1).qualifiers().contains(BeforeDestroyed.Literal.REQUEST));
        assertTrue(listener.events.get(2).qualifiers().contains(Destroyed.Literal.REQUEST));
        assertTrue(listener.events.get(3).qualifiers().contains(Initialized.Literal.REQUEST));
        assertTrue(listener.events.get(4).qualifiers().contains(BeforeDestroyed.Literal.REQUEST));
        assertTrue(listener.events.get(5).qualifiers().contains(Destroyed.Literal.REQUEST));
        assertTrue(listener.events.get(6).qualifiers().contains(Initialized.Literal.REQUEST));
        assertTrue(listener.events.get(7).qualifiers().contains(BeforeDestroyed.Literal.REQUEST));
        assertTrue(listener.events.get(8).qualifiers().contains(Destroyed.Literal.REQUEST));
    }

    @WebSocket(path = "/append-blocking")
    public static class AppendBlocking {

        @Inject
        RequestScopedBean bean;

        @OnTextMessage
        String process(String message) throws InterruptedException {
            return bean.appendId(message);
        }
    }

    @WebSocket(path = "/append")
    public static class Append {

        @Inject
        RequestScopedBean bean;

        @OnTextMessage
        Uni<String> process(String message) throws InterruptedException {
            return Uni.createFrom().item(() -> bean.appendId(message));
        }
    }

}
