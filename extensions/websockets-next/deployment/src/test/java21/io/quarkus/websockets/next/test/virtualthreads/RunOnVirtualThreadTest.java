package io.quarkus.websockets.next.test.virtualthreads;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.vertx.VirtualThreadsAssertions;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

public class RunOnVirtualThreadTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Endpoint.class, WSClient.class, RequestScopedBean.class)
                        .addAsResource(new StringAsset(
                                "quarkus.virtual-threads.name-prefix=wsnext-virtual-thread-"),
                                "application.properties");
            });

    @Inject
    Vertx vertx;

    @TestHTTPResource("end")
    URI endUri;

    @TestHTTPResource("virt-on-class")
    URI onClassUri;

    @Test
    void testVirtualThreads() {
        try (WSClient client = new WSClient(vertx).connect(endUri)) {
            client.sendAndAwait("foo");
            client.sendAndAwait("bar");
            client.waitForMessages(2);
            String message1 = client.getMessages().get(0).toString();
            String message2 = client.getMessages().get(1).toString();
            assertNotEquals(message1, message2);
            assertTrue(message1.startsWith("wsnext-virtual-thread-"));
            assertTrue(message2.startsWith("wsnext-virtual-thread-"));
        }
    }

    @Test
    void testVirtualThreadsOnClass() {
        try (WSClient client = new WSClient(vertx).connect(onClassUri)) {
            client.sendAndAwait("foo");
            client.sendAndAwait("bar");
            client.waitForMessages(3);
            String open = client.getMessages().get(0).toString();
            String message1 = client.getMessages().get(1).toString();
            String message2 = client.getMessages().get(2).toString();
            assertNotEquals(open, message1, message2);
            assertTrue(open.startsWith("wsnext-virtual-thread-"));
            assertTrue(message1.startsWith("wsnext-virtual-thread-"));
            assertTrue(message2.startsWith("wsnext-virtual-thread-"));
        }
    }

    @WebSocket(path = "/end")
    public static class Endpoint {

        @Inject
        RequestScopedBean bean;

        @RunOnVirtualThread
        @OnTextMessage
        String text(String ignored) {
            VirtualThreadsAssertions.assertEverything();
            return Thread.currentThread().getName();
        }

        @OnError
        String error(Throwable t) {
            return t.toString();
        }

    }

    @RunOnVirtualThread
    @WebSocket(path = "/virt-on-class")
    public static class EndpointVirtOnClass {

        @Inject
        RequestScopedBean bean;

        @OnOpen
        String open() {
            VirtualThreadsAssertions.assertEverything();
            return Thread.currentThread().getName();
        }

        @OnTextMessage
        String text(String ignored) {
            VirtualThreadsAssertions.assertEverything();
            return Thread.currentThread().getName();
        }
    }

    @RequestScoped
    public static class RequestScopedBean {
        
    }

}
