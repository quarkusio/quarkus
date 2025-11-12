package io.quarkus.redis.deployment.client;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;

@QuarkusTestResource(RedisTestResource.class)
public class RedisInactiveClientsByConfigExceptionTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.redis.active.active", "false")
            .overrideConfigKey("quarkus.redis.active.hosts", "${quarkus.redis.tr}/1")
            .assertException(e -> assertThat(e)// Can't use isInstanceOf due to weird classloading in tests
                    .satisfies(t -> assertThat(t.getClass().getName()).isEqualTo(InactiveBeanException.class.getName()))
                    .hasMessageContainingAll(
                            """
                                    Redis Client 'active' was deactivated through configuration properties. \
                                    To activate the Redis Client, set configuration property \
                                    'quarkus.redis.active.active' to 'true' and configure the Redis Client 'active'. \
                                    Refer to https://quarkus.io/guides/redis-reference for guidance.
                                    """));

    @Inject
    @RedisClientName("active")
    Redis active;
    @Inject
    @RedisClientName("active")
    io.vertx.redis.client.Redis activeBare;
    @Inject
    @RedisClientName("active")
    RedisAPI activeApi;
    @Inject
    @RedisClientName("active")
    io.vertx.redis.client.RedisAPI activeBareApi;
    @Inject
    @RedisClientName("active")
    RedisDataSource activeDataSource;
    @Inject
    @RedisClientName("active")
    ReactiveRedisDataSource activeReactiveDataSource;

    @Test
    void inactive() {

    }
}
