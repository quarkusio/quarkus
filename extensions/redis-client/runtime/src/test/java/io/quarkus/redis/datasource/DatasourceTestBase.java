package io.quarkus.redis.datasource;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;

@ExtendWith(RedisServerExtension.class)
public class DatasourceTestBase {

    final String key = UUID.randomUUID().toString();
    static Redis redis;
    static Vertx vertx;
    static RedisAPI api;

    @BeforeAll
    static void init() {
        redis = RedisServerExtension.redis;
        vertx = RedisServerExtension.vertx;
        api = RedisServerExtension.api;
    }

}
