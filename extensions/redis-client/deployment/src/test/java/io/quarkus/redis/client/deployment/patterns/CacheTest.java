package io.quarkus.redis.client.deployment.patterns;

import static org.awaitility.Awaitility.await;

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
public class CacheTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class).addClass(MyRedisCache.class).addClass(BusinessObject.class))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}");

    @Inject
    MyRedisCache cache;

    @Test
    void cache() {
        BusinessObject foo = cache.get("cache-foo");
        BusinessObject bar = cache.get("cache-bar");
        Assertions.assertNull(foo);
        Assertions.assertNull(bar);

        cache.set("cache-foo", new BusinessObject("cache-foo"));
        cache.set("cache-bar", new BusinessObject("cache-bar"));

        foo = cache.get("cache-foo");
        bar = cache.get("cache-bar");

        Assertions.assertNotNull(foo);
        Assertions.assertNotNull(bar);

        await().until(() -> cache.get("cache-bar") == null);

        foo = cache.get("cache-foo");
        bar = cache.get("cache-bar");

        Assertions.assertNull(foo);
        Assertions.assertNull(bar);
    }

    public static final class BusinessObject {
        public String result;

        public BusinessObject() {

        }

        public BusinessObject(String v) {
            this.result = v;
        }
    }

    @ApplicationScoped
    public static class MyRedisCache {

        private final ValueCommands<String, BusinessObject> commands;

        public MyRedisCache(RedisDataSource ds) {
            commands = ds.value(BusinessObject.class);
        }

        public BusinessObject get(String key) {
            return commands.get(key);
        }

        public void set(String key, BusinessObject bo) {
            commands.setex(key, 1, bo); // Expires after 1 second
        }

    }

}
