package io.quarkus.redis.client.deployment.datasource;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.client.deployment.RedisTestResource;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(RedisTestResource.class)
public class DataSourceInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClass(MyRedisApp.class))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.my-redis.hosts", "${quarkus.redis.tr}");;

    @Inject
    MyRedisApp app;

    @Test
    public void testDefault() {
        String s = UUID.randomUUID().toString();
        app.set(s);
        Assertions.assertEquals(s, app.get());
    }

    @Test
    public void testMyRedis() {
        String s = UUID.randomUUID().toString();
        app.setMyRedis(s);
        Assertions.assertEquals(s, app.getMyRedis());
    }

    @ApplicationScoped
    public static class MyRedisApp {

        @Inject
        RedisDataSource blocking;
        @Inject
        ReactiveRedisDataSource reactive;

        @Inject
        @RedisClientName("my-redis")
        RedisDataSource myRedisBlocking;
        @Inject
        @RedisClientName("my-redis")
        ReactiveRedisDataSource myRedisReactive;

        public void set(String data) {
            reactive.value(String.class, String.class)
                    .set("foo", data)
                    .await().indefinitely();
        }

        public String get() {
            return blocking.value(String.class, String.class)
                    .get("foo");
        }

        public void setMyRedis(String data) {
            myRedisReactive.value(String.class, String.class)
                    .set("foo", data)
                    .await().indefinitely();
        }

        public String getMyRedis() {
            return myRedisBlocking.value(String.class, String.class)
                    .get("foo");
        }

    }
}
