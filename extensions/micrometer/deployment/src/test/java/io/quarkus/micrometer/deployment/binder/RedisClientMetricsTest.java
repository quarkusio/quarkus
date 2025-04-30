
package io.quarkus.micrometer.deployment.binder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;

@DisabledOnOs(OS.WINDOWS)
public class RedisClientMetricsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest();

    final static SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @BeforeAll
    static void setRegistry() {
        Metrics.addRegistry(registry);
    }

    @AfterAll()
    static void removeRegistry() {
        Metrics.removeRegistry(registry);
    }

    @Inject
    RedisDataSource ds;

    @Inject
    Redis redis;

    @Test
    void testCommands() {
        double count = 0.0;
        double fail = 0.0;
        double succ = 0.0;

        try {
            Counter failures = registry.get("redis.commands.failure").counter();
            Counter counter = registry.get("redis.commands.count").counter();
            Counter success = registry.get("redis.commands.success").counter();
            count = counter.count();
            fail = failures.count();
            succ = success.count();
        } catch (MeterNotFoundException e) {
            // ignore it
        }

        ds.value(Integer.class).incr("counter");
        ds.value(String.class).set("foo", "bar");

        Assertions.assertEquals(count + 2, registry.get("redis.commands.count").counter().count());
        Assertions.assertEquals(succ + 2, registry.get("redis.commands.success").counter().count());
        Assertions.assertEquals(fail + 0, registry.get("redis.commands.failure").counter().count());

        Assertions.assertEquals(count + 2, registry.get("redis.commands.duration").timer().count());
        Assertions.assertTrue(registry.get("redis.commands.duration").timer().mean(TimeUnit.NANOSECONDS) > 0);

        Assertions.assertThrows(Exception.class, () -> ds.value(String.class).incr("foo"));
        Assertions.assertEquals(fail + 1, registry.get("redis.commands.failure").counter().count());

        // Verify we have TCP client metrics
        Assertions.assertNotNull(registry.get("redis.connections").longTaskTimer());
        Assertions.assertNotNull(registry.get("redis.bytes.read").summary());
        Assertions.assertNotNull(registry.get("redis.bytes.written").summary());
    }

    @Test
    void testBatch() {
        double count = 0.0;
        double fail = 0.0;
        double succ = 0.0;

        try {
            Counter failures = registry.get("redis.commands.failure").counter();
            Counter counter = registry.get("redis.commands.count").counter();
            Counter success = registry.get("redis.commands.success").counter();
            count = counter.count();
            fail = failures.count();
            succ = success.count();
        } catch (MeterNotFoundException e) {
            // ignore it
        }

        redis.batch(List.of(
                Request.cmd(Command.SET).arg("b1").arg("value"),
                Request.cmd(Command.SET).arg("b2").arg("value"),
                Request.cmd(Command.INCR).arg("c1"))).toCompletionStage().toCompletableFuture().join();

        Assertions.assertEquals(count + 1, registry.get("redis.commands.count").counter().count());
        Assertions.assertEquals(succ + 1, registry.get("redis.commands.success").counter().count());
        Assertions.assertEquals(fail + 0, registry.get("redis.commands.failure").counter().count());

        Assertions.assertEquals(count + 1, registry.get("redis.commands.duration").timer().count());
        Assertions.assertTrue(registry.get("redis.commands.duration").timer().mean(TimeUnit.NANOSECONDS) > 0);

        // Verify we have TCP client metrics
        Assertions.assertNotNull(registry.get("redis.connections").longTaskTimer());
        Assertions.assertNotNull(registry.get("redis.bytes.read").summary());
        Assertions.assertNotNull(registry.get("redis.bytes.written").summary());
    }

}
