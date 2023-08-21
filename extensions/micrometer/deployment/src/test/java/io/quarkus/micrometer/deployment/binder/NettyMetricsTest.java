package io.quarkus.micrometer.deployment.binder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.common.docs.KeyName;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.netty4.NettyAllocatorMetrics;
import io.micrometer.core.instrument.binder.netty4.NettyEventExecutorMetrics;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import io.quarkus.micrometer.test.HelloResource;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.impl.VertxByteBufAllocator;
import io.vertx.core.impl.VertxInternal;

public class NettyMetricsTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(HelloResource.class))
            .withConfigurationResource("test-logging.properties")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.netty.enabled", "true")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false");

    @Inject
    @Any
    Instance<MeterBinder> binders;

    @Inject
    MeterRegistry registry;

    @Inject
    Vertx vertx;

    private static final Set<Tag> NAM_PBBA_TAGS = Tags.of(
            "id", String.valueOf(PooledByteBufAllocator.DEFAULT.hashCode()),
            "allocator.type", "PooledByteBufAllocator")
            .stream()
            .collect(Collectors.toSet());

    private static final Set<Tag> NAM_UNPBBA_TAGS = Tags.of(
            "id", String.valueOf(UnpooledByteBufAllocator.DEFAULT.hashCode()),
            "allocator.type", "UnpooledByteBufAllocator")
            .stream()
            .collect(Collectors.toSet());

    private static final Set<Tag> VX_NAM_PBBA_TAGS = Tags.of(
            "id", String.valueOf(VertxByteBufAllocator.POOLED_ALLOCATOR.hashCode()),
            "allocator.type", "PooledByteBufAllocator")
            .stream()
            .collect(Collectors.toSet());

    private static final Set<Tag> VX_NAM_UNPBBA_TAGS = Tags.of(
            "id", String.valueOf(VertxByteBufAllocator.UNPOOLED_ALLOCATOR.hashCode()),
            "allocator.type", "UnpooledByteBufAllocator")
            .stream()
            .collect(Collectors.toSet());

    private static final Tag HEAP_MEMORY = Tag.of(AllocatorMemoryKeyNames.MEMORY_TYPE.asString(), "heap");
    private static final Tag DIRECT_MEMORY = Tag.of(AllocatorMemoryKeyNames.MEMORY_TYPE.asString(), "direct");

    enum AllocatorKeyNames implements KeyName {
        ID {
            public String asString() {
                return "id";
            }
        },
        ALLOCATOR_TYPE {
            public String asString() {
                return "allocator.type";
            }
        };
    }

    enum AllocatorMemoryKeyNames implements KeyName {
        MEMORY_TYPE {
            public String asString() {
                return "memory.type";
            }
        };
    }

    private void testNettyMetrics(long expected, Class<? extends MeterBinder> mbClass) {
        Assertions.assertFalse(binders.isUnsatisfied());
        long count = binders.stream()
                .filter(mbClass::isInstance)
                .count();
        Assertions.assertEquals(expected, count);
    }

    private static Double getValue(List<Meter> meters, Set<Tag> expected) {
        for (Meter meter : meters) {
            List<Tag> tags = meter.getId().getTags();
            if (tags.containsAll(expected)) {
                return meter.match(Gauge::value, null, null, null, null, null, null, null, null);
            }
        }
        return null;
    }

    private static Set<Tag> tags(Set<Tag> tags, Tag tag) {
        Set<Tag> newTags = new HashSet<>(tags);
        newTags.add(tag);
        return newTags;
    }

    private void testAllocatorMetricsValues(Set<Tag> tags) {
        List<Meter> meters = registry.getMeters();

        Double heap0 = getValue(meters, tags(tags, HEAP_MEMORY));
        Assertions.assertNotNull(heap0);
        Double direct0 = getValue(meters, tags(tags, DIRECT_MEMORY));
        Assertions.assertNotNull(direct0);

        RestAssured.get("/hello/Netty").then().body(Matchers.equalTo("hello Netty"));

        Double heap1 = getValue(meters, tags(tags, HEAP_MEMORY));
        Double direct1 = getValue(meters, tags(tags, DIRECT_MEMORY));

        Assertions.assertTrue(heap0 <= heap1);
        Assertions.assertTrue(direct0 <= direct1);
    }

    @Test
    public void testNettyAllocatorMetrics() {
        testNettyMetrics(5L, NettyAllocatorMetrics.class);
    }

    @Test
    public void testNettyEventExecutorMetrics() {
        testNettyMetrics(2L, NettyEventExecutorMetrics.class);
    }

    @Test
    public void testAllocatorMetricsValues() {
        testAllocatorMetricsValues(NAM_PBBA_TAGS);
        testAllocatorMetricsValues(NAM_UNPBBA_TAGS);
        testAllocatorMetricsValues(VX_NAM_PBBA_TAGS);
        testAllocatorMetricsValues(VX_NAM_UNPBBA_TAGS);
    }

    @Test
    @Timeout(60L)
    public void testEventExecutorMetricsValues() throws Exception {
        VertxInternal vi = (VertxInternal) vertx;
        assertEventGroup(vi.getEventLoopGroup());
        assertEventGroup(vi.getAcceptorEventLoopGroup());
    }

    private void assertEventGroup(EventLoopGroup group) throws Exception {
        int tasks = 0;
        for (EventExecutor ee : group) {
            tasks++;
        }
        final CyclicBarrier allPendingTasksAreIn = new CyclicBarrier(tasks + 1);
        CountDownLatch waitCollectingMeasures = new CountDownLatch(1);
        List<Future<Future<?>>> pendingTasksCompleted = new ArrayList<>(tasks);
        for (EventExecutor eventLoop : group) {
            pendingTasksCompleted.add(eventLoop.submit(() -> {
                try {
                    Future<?> pendingTask = eventLoop.submit(() -> {
                    });
                    // this executor will have 1 pending task because is still running this current one
                    allPendingTasksAreIn.await();
                    waitCollectingMeasures.await();
                    return pendingTask;
                } catch (Throwable ignore) {
                    return null;
                }
            }));
        }
        allPendingTasksAreIn.await();
        List<Meter> meters = registry.getMeters();
        // this would return 1 for everyone
        for (EventExecutor eventLoop : group) {
            checkMetrics(meters, eventLoop, 1);
        }
        waitCollectingMeasures.countDown();
        for (Future<Future<?>> pendingTaskCompleted : pendingTasksCompleted) {
            pendingTaskCompleted.get().get();
        }
        // this would return 0 for everyone
        for (EventExecutor eventLoop : group) {
            checkMetrics(meters, eventLoop, 0);
        }
    }

    private void checkMetrics(List<Meter> meters, EventExecutor executor, int expected) {
        if (executor instanceof SingleThreadEventExecutor) {
            SingleThreadEventExecutor stee = (SingleThreadEventExecutor) executor;

            int pendingTasks = stee.pendingTasks();
            Assertions.assertEquals(expected, pendingTasks);

            Tag tag = Tag.of("name", stee.threadProperties().name());
            Set<Tag> tags = Set.of(tag);

            Double metricsValue = getValue(meters, tags);
            Assertions.assertNotNull(metricsValue);
            int mvInt = metricsValue.intValue();
            Assertions.assertEquals(expected, mvInt);
        }
    }
}