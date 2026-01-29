package io.quarkus.reactive.pg.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactive.datasource.runtime.ConnectionStealingMonitor;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgPool;

public class StealingMonitorTest {

    private static final String SYS_PROP_KEY = "quarkus.reactive-datasource.monitor-stealing";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> {
                // Set System Property BEFORE app starts
                System.setProperty(SYS_PROP_KEY, "true");

                return ShrinkWrap.create(JavaArchive.class)
                        .addClasses(MyTestMonitor.class);
            })
            // Standard Datasource Config (Application Properties)
            .overrideConfigKey("quarkus.datasource.db-kind", "postgresql")
            .overrideConfigKey("quarkus.datasource.username", "quarkus")
            .overrideConfigKey("quarkus.datasource.password", "quarkus")
            .overrideConfigKey("quarkus.datasource.jdbc", "false");

    @Inject
    PgPool pool;

    @Inject
    Vertx vertx;

    @Inject
    MyTestMonitor monitor;

    @AfterAll
    public static void tearDown() {
        // Clean up the system property to avoid side effects on other tests
        System.clearProperty(SYS_PROP_KEY);
    }

    @Test
    public void testMonitorInterception() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        vertx.runOnContext(v -> {
            pool.withConnection(sqlConnection -> {
                return sqlConnection.query("SELECT 1").execute();
            }).onComplete(ar -> {
                if (ar.failed()) {
                    ar.cause().printStackTrace();
                }
                latch.countDown();
            });
        });

        assertTrue(latch.await(5, TimeUnit.MINUTES), "Query timed out");

        assertFalse(monitor.events.isEmpty(),
                "Monitor should have received an event. Check if System Property was picked up.");

        MonitorEvent event = monitor.events.get(0);
        assertEquals("<default>", event.datasourceName);
    }

    // --- Test Helpers ---

    @Singleton
    public static class MyTestMonitor implements ConnectionStealingMonitor {
        public final List<MonitorEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public void connectionAcquired(String datasourceName, boolean stolen) {
            events.add(new MonitorEvent(datasourceName, stolen));
        }
    }

    public static class MonitorEvent {
        public final String datasourceName;
        public final boolean stolen;

        public MonitorEvent(String datasourceName, boolean stolen) {
            this.datasourceName = datasourceName;
            this.stolen = stolen;
        }

        @Override
        public String toString() {
            return "MonitorEvent{datasource='" + datasourceName + "', stolen=" + stolen + "}";
        }
    }
}