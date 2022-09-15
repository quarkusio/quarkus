package io.quarkus.redis.client.deployment.patterns;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.client.deployment.RedisTestResource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(RedisTestResource.class)
public class CounterTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(MyRedisCounter.class))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}");

    @Inject
    MyRedisCounter counter;

    @Test
    void counter() {

        long v1 = counter.get("counter-foo");
        long v2 = counter.get("counter-bar");

        Assertions.assertEquals(0, v1);
        Assertions.assertEquals(0, v2);

        counter.incr("counter-foo");
        counter.incr("counter-foo");
        counter.incr("counter-bar");
        counter.incr("counter-foo");
        counter.incr("counter-bar");

        Assertions.assertEquals(3, counter.get("counter-foo"));
        Assertions.assertEquals(2, counter.get("counter-bar"));
    }

    @ApplicationScoped
    public static class MyRedisCounter {

        private final ValueCommands<String, Long> commands;

        public MyRedisCounter(RedisDataSource ds) {
            commands = ds.value(Long.class);
        }

        public long get(String key) {
            Long l = commands.get(key);
            if (l == null) {
                return 0L;
            }
            return l;
        }

        public void incr(String key) {
            commands.incr(key);
        }

    }

}
