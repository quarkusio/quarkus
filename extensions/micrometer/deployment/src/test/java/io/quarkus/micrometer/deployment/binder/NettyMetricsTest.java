package io.quarkus.micrometer.deployment.binder;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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

    private void testNettyMetrics(long expected, Class<? extends MeterBinder> mbClass) {
        Assertions.assertFalse(binders.isUnsatisfied());
        long count = binders.stream()
                .filter(mbClass::isInstance)
                .count();
        Assertions.assertEquals(expected, count);
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
        RestAssured.get("/hello/Netty").then().body(Matchers.equalTo("hello Netty"));

        boolean pbba_found = false;
        boolean unpbba_found = false;
        boolean vx_pbba_found = false;
        boolean vx_unpbba_found = false;
        List<Meter> meters = registry.getMeters();
        for (Meter meter : meters) {
            List<Tag> tags = meter.getId().getTags();
            if (tags.containsAll(NAM_PBBA_TAGS)) {
                pbba_found = true;
            }
            if (tags.containsAll(NAM_UNPBBA_TAGS)) {
                unpbba_found = true;
            }
            if (tags.containsAll(VX_NAM_PBBA_TAGS)) {
                vx_pbba_found = true;
            }
            if (tags.containsAll(VX_NAM_UNPBBA_TAGS)) {
                vx_unpbba_found = true;
            }
        }
        Assertions.assertTrue(pbba_found);
        Assertions.assertTrue(unpbba_found);
        Assertions.assertTrue(vx_pbba_found);
        Assertions.assertTrue(vx_unpbba_found);
    }

    @Test
    public void testEventExecutorMetricsValues() {
        VertxInternal vi = (VertxInternal) vertx;
        assertEventGroup(vi.getEventLoopGroup());
        assertEventGroup(vi.getAcceptorEventLoopGroup());
    }

    private void assertEventGroup(EventLoopGroup eventLoopGroup) {
        for (EventExecutor eventExecutor : eventLoopGroup) {
            if (eventExecutor instanceof SingleThreadEventExecutor) {
                SingleThreadEventExecutor singleThreadEventExecutor = (SingleThreadEventExecutor) eventExecutor;
                Tag tag = Tag.of("name", singleThreadEventExecutor.threadProperties().name());
                boolean found = false;
                List<Meter> meters = registry.getMeters();
                for (Meter meter : meters) {
                    List<Tag> tags = meter.getId().getTags();
                    if (tags.contains(tag)) {
                        found = true;
                    }
                }
                Assertions.assertTrue(found);
            }
        }
    }
}