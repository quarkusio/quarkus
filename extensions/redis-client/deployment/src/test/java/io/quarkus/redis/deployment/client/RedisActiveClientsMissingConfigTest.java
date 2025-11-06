package io.quarkus.redis.deployment.client;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
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
public class RedisActiveClientsMissingConfigTest {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .assertException(e -> assertThat(e)// Can't use isInstanceOf due to weird classloading in tests
                    .satisfies(t -> assertThat(t.getClass().getName()).isEqualTo(InactiveBeanException.class.getName()))
                    .hasMessageContainingAll(
                            """
                                    Redis Client 'active' was deactivated automatically because neither the \
                                    hosts nor the hostsProviderName is set. \
                                    To activate the Redis Client, set the configuration property 'quarkus.redis.active.hosts' \
                                    or 'quarkus.redis.active.hosts-provider-name'. \
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
    void missingConfig() {
        Assertions.fail();
    }
}
