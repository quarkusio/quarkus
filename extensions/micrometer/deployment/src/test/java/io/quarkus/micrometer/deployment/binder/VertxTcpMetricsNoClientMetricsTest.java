package io.quarkus.micrometer.deployment.binder;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.net.NetSocket;

public class VertxTcpMetricsNoClientMetricsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .withApplicationRoot((jar) -> jar
                    .addClasses(NetServer.class, NetClient.class));

    @Inject
    NetClient client;

    @Inject
    NetServer server;

    final static SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @BeforeAll
    static void setRegistry() {
        Metrics.addRegistry(registry);
    }

    @AfterAll()
    static void removeRegistry() {
        Metrics.removeRegistry(registry);
    }

    private Search getMeter(String name) {
        return registry.find(name);
    }

    @Test
    void testTcpMetricsWithoutClientMetrics() {
        server.start();
        try {
            Assertions.assertEquals("HELLO", client.sendAndAwait("hello"));
            Assertions.assertEquals("HOW ARE YOU?", client.sendAndAwait("How are you?"));

            await().untilAsserted(() -> Assertions.assertEquals(1, getMeter("tcp.connections").longTaskTimer().activeTasks()));
            await().untilAsserted(() -> Assertions.assertNull(getMeter("telnet.connections").longTaskTimer()));

            client.quit();

            await().untilAsserted(() -> Assertions.assertEquals(0, getMeter("tcp.connections").longTaskTimer().activeTasks()));
            await().until(() -> getMeter("tcp.bytes.written").summary().totalAmount() > 0);
            await().until(() -> getMeter("tcp.bytes.read").summary().totalAmount() > 0);

            await().untilAsserted(() -> Assertions.assertNull(getMeter("telnet.connections").longTaskTimer()));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            server.stop();
        }
    }

    @ApplicationScoped
    static class NetServer {

        @Inject
        Vertx vertx;
        private io.vertx.mutiny.core.net.NetServer server;

        public void start() {
            server = vertx.createNetServer()
                    .connectHandler(socket -> {
                        socket.handler(buffer -> {
                            if (buffer.toString().equalsIgnoreCase("exit")) {
                                socket.endAndForget();
                            } else {
                                socket.writeAndForget(buffer.toString().toUpperCase());
                            }
                        });
                    })
                    .listenAndAwait(8888);
        }

        public void stop() {
            server.closeAndAwait();
        }

    }

    @ApplicationScoped
    static class NetClient {
        @Inject
        Vertx vertx;
        private NetSocket client;

        private BlockingQueue<String> queue = new ArrayBlockingQueue<>(1);

        @PostConstruct
        public void init() {
            // Do not pass a name so the metrics clients are not reported.
            client = vertx.createNetClient()
                    .connect(8888, "localhost")
                    .await().indefinitely();
            client.handler(buffer -> queue.offer(buffer.toString()));
        }

        public String sendAndAwait(String message) throws InterruptedException {
            client.writeAndAwait(message);
            return queue.take();
        }

        public void quit() {
            client.closeAndAwait();
        }
    }

}
