package io.quarkus.websockets.next.test.sessioncontext;

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
import io.vertx.core.Vertx;

public class SessionContextTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Append.class, WSClient.class, SessionScopedBean.class, SessionContextListener.class);
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("append")
    URI appendUri;

    @Inject
    SessionContextListener listener;

    @Test
    void testSessionContext() throws InterruptedException {
        WSClient client = WSClient.create(vertx).connect(appendUri);
        client.send("foo");
        client.send("bar");
        client.send("baz");
        client.waitForMessages(3);
        assertEquals("foo", client.getMessages().get(0).toString());
        assertEquals("foobar", client.getMessages().get(1).toString());
        assertEquals("foobarbaz", client.getMessages().get(2).toString());
        client.disconnect();
        assertTrue(listener.destroyLatch.await(5, TimeUnit.SECONDS));
        assertTrue(SessionScopedBean.DESTROYED.get());
        assertEquals(3, listener.events.size());
        assertEquals(listener.events.get(0).payload(), listener.events.get(1).payload());
        assertEquals(listener.events.get(1).payload(), listener.events.get(2).payload());
        assertTrue(listener.events.get(0).qualifiers().contains(Initialized.Literal.SESSION));
        assertTrue(listener.events.get(1).qualifiers().contains(BeforeDestroyed.Literal.SESSION));
        assertTrue(listener.events.get(2).qualifiers().contains(Destroyed.Literal.SESSION));
    }

    @WebSocket(path = "/append")
    public static class Append {

        @Inject
        SessionScopedBean bean;

        @OnTextMessage
        String process(String message) throws InterruptedException {
            return bean.appendAndGet(message);
        }
    }

}
