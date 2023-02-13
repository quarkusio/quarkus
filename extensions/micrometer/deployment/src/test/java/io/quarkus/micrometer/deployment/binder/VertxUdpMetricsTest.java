package io.quarkus.micrometer.deployment.binder;

import static org.awaitility.Awaitility.await;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.search.Search;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.datagram.DatagramSocket;

public class VertxUdpMetricsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .withApplicationRoot((jar) -> jar
                    .addClasses(ParticipantA.class, ParticipantB.class));

    @Inject
    ParticipantA clientA;

    @Inject
    ParticipantB clientB;

    private Search getMeter(String name) {
        return Metrics.globalRegistry.find(name);
    }

    @Test
    void testUdpMetrics() {
        clientA.start();
        clientB.start();
        try {
            await().until(clientB::isDone);
            Assertions.assertTrue(Metrics.globalRegistry.find("udp.bytes.read").tags("address", "127.0.0.1:8888").summary()
                    .totalAmount() > 0);
            Assertions.assertTrue(Metrics.globalRegistry.find("udp.bytes.read").tags("address", "127.0.0.1:8889").summary()
                    .totalAmount() > 0);
            Assertions.assertNotNull(
                    Metrics.globalRegistry.find("udp.bytes.written").tags("address", "127.0.0.1:8889").summary());
            Assertions.assertNotNull(
                    Metrics.globalRegistry.find("udp.bytes.written").tags("address", "127.0.0.1:8888").summary());
        } finally {
            clientA.stop();
            clientB.stop();
        }
    }

    @ApplicationScoped
    static class ParticipantA {

        @Inject
        Vertx vertx;
        private DatagramSocket server;

        public void start() {
            server = vertx.createDatagramSocket()
                    .handler(packet -> {
                        server.sendAndForget(packet.data().toString().toUpperCase(), packet.sender().port(),
                                packet.sender().host());
                    })
                    .listenAndAwait(8888, "localhost");
        }

        public void stop() {
            server.closeAndAwait();
        }

    }

    @ApplicationScoped
    static class ParticipantB {

        @Inject
        Vertx vertx;
        private DatagramSocket server;

        private volatile boolean received;

        public void start() {
            server = vertx.createDatagramSocket()
                    .handler(packet -> received = true)
                    .listenAndAwait(8889, "localhost");

            server.sendAndAwait("hello", 8888, "localhost");
        }

        public void stop() {
            server.closeAndAwait();
        }

        public boolean isDone() {
            return received;
        }
    }

}
