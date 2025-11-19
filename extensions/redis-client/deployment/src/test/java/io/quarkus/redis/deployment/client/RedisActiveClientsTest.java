package io.quarkus.redis.deployment.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;

@QuarkusTestResource(RedisTestResource.class)
public class RedisActiveClientsTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.redis.active.hosts", "${quarkus.redis.tr}/1");

    @Inject
    InjectableInstance<Redis> defaultActive;
    @Inject
    InjectableInstance<io.vertx.redis.client.Redis> defaultActiveBare;
    @Inject
    InjectableInstance<RedisAPI> defaultActiveApi;
    @Inject
    InjectableInstance<io.vertx.redis.client.RedisAPI> defaultActiveBareApi;
    @Inject
    InjectableInstance<RedisDataSource> defaultActiveDataSource;
    @Inject
    InjectableInstance<ReactiveRedisDataSource> defaultActiveReactiveDataSource;

    @Inject
    @RedisClientName("active")
    InjectableInstance<Redis> active;
    @Inject
    @RedisClientName("active")
    InjectableInstance<io.vertx.redis.client.Redis> activeBare;
    @Inject
    @RedisClientName("active")
    InjectableInstance<RedisAPI> activeApi;
    @Inject
    @RedisClientName("active")
    InjectableInstance<io.vertx.redis.client.RedisAPI> activeBareApi;
    @Inject
    @RedisClientName("active")
    InjectableInstance<RedisDataSource> activeDataSource;
    @Inject
    @RedisClientName("active")
    InjectableInstance<ReactiveRedisDataSource> activeReactiveDataSource;

    @Inject
    @Any
    InjectableInstance<Redis> all;
    @Inject
    @Any
    InjectableInstance<io.vertx.redis.client.Redis> allBare;
    @Inject
    @Any
    InjectableInstance<RedisAPI> allApi;
    @Inject
    @Any
    InjectableInstance<io.vertx.redis.client.RedisAPI> allBareApi;
    @Inject
    @Any
    InjectableInstance<RedisDataSource> allDataSource;
    @Inject
    @Any
    InjectableInstance<ReactiveRedisDataSource> allReactiveDataSource;

    @Test
    void activeClients() {
        assertEquals(1, defaultActive.listActive().size());
        assertEquals(1, defaultActiveBare.listActive().size());
        assertEquals(1, defaultActiveApi.listActive().size());
        assertEquals(1, defaultActiveBareApi.listActive().size());
        assertEquals(1, defaultActiveDataSource.listActive().size());
        assertEquals(1, defaultActiveReactiveDataSource.listActive().size());

        assertEquals(1, active.listActive().size());
        assertEquals(1, activeBare.listActive().size());
        assertEquals(1, activeApi.listActive().size());
        assertEquals(1, activeBareApi.listActive().size());
        assertEquals(1, activeDataSource.listActive().size());
        assertEquals(1, activeReactiveDataSource.listActive().size());

        assertEquals(2, all.listActive().size());
        assertEquals(2, allBare.listActive().size());
        assertEquals(2, allApi.listActive().size());
        assertEquals(2, allBareApi.listActive().size());
        assertEquals(2, allDataSource.listActive().size());
        assertEquals(2, allReactiveDataSource.listActive().size());
    }
}
