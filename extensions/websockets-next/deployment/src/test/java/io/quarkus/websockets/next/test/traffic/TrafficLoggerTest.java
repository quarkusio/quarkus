package io.quarkus.websockets.next.test.traffic;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;

public abstract class TrafficLoggerTest {

    @TestHTTPResource("/")
    URI endUri;

    @WebSocket(path = "/end")
    public static class Endpoint {

        static final CountDownLatch CLOSED_LATCH = new CountDownLatch(1);

        @OnOpen
        public String open() {
            return "ok";
        }

        @OnClose
        public void close() {
            CLOSED_LATCH.countDown();
        }

    }

    static void addApplicationProperties(JavaArchive archive, boolean server) {
        archive.addAsResource(new StringAsset(
                "quarkus.websockets-next." + (server ? "server" : "client") + ".traffic-logging.enabled=true\n"
                        + "quarkus.log.category.\"io.quarkus.websockets.next.traffic\".level=DEBUG"),
                "application.properties");
    }

    static Consumer<List<LogRecord>> logRecordsConsumer(boolean received) {
        return recs -> {
            assertTrue(recs.stream()
                    .anyMatch(r -> r.getMessage().contains("%s connection opened:")));
            assertTrue(recs.stream()
                    .anyMatch(r -> r.getMessage()
                            .contains("%s " + (received ? "received" : "sent") + " text message, Connection[%s]")));
            assertTrue(recs.stream()
                    .anyMatch(r -> r.getMessage().contains("%s connection closed,")));
        };
    }

    static boolean isTrafficLogRecord(LogRecord r) {
        return r.getLevel().equals(Level.FINE)
                && r.getLoggerName().equals("io.quarkus.websockets.next.traffic");
    }

}
