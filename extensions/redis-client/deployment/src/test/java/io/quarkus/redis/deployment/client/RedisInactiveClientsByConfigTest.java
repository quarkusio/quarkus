package io.quarkus.redis.deployment.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

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
public class RedisInactiveClientsByConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.redis.inactive.active", "false")
            .overrideConfigKey("quarkus.redis.inactive.hosts", "${quarkus.redis.tr}/1");

    @Inject
    @RedisClientName("inactive")
    InjectableInstance<Redis> inactive;
    @Inject
    @RedisClientName("inactive")
    InjectableInstance<io.vertx.redis.client.Redis> inactiveBare;
    @Inject
    @RedisClientName("inactive")
    InjectableInstance<RedisAPI> inactiveApi;
    @Inject
    @RedisClientName("inactive")
    InjectableInstance<io.vertx.redis.client.RedisAPI> inactiveBareApi;
    @Inject
    @RedisClientName("inactive")
    InjectableInstance<RedisDataSource> inactiveDataSource;
    @Inject
    @RedisClientName("inactive")
    InjectableInstance<ReactiveRedisDataSource> inactiveReactiveDataSource;

    @Test
    void inactiveClients() {
        assertEquals(0, inactive.listActive().size());
        assertEquals(0, inactiveBare.listActive().size());
        assertEquals(0, inactiveApi.listActive().size());
        assertEquals(0, inactiveBareApi.listActive().size());
        assertEquals(0, inactiveDataSource.listActive().size());
        assertEquals(0, inactiveReactiveDataSource.listActive().size());
    }
}
