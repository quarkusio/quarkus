package io.quarkus.redis.it.lettuce.withoutreactor;

import jakarta.inject.Inject;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Uses the Lettuce backend through the data source and through the Lettuce connection bean, with the {@code reactive}
 * argument, requests Lettuce's reactive API instead.
 */
@QuarkusMain
public class LettuceWithoutReactorApp implements QuarkusApplication {

    @Inject
    RedisDataSource dataSource;

    @Inject
    StatefulRedisConnection<String, String> connection;

    @Override
    public int run(String... args) {
        if (args.length > 0 && "reactive".equals(args[0])) {
            return requestReactiveApi();
        }

        dataSource.value(String.class).set("lettuce-without-reactor", "value");
        String value = dataSource.value(String.class).get("lettuce-without-reactor");
        if (!"value".equals(value)) {
            System.err.println("data source: expected value, got " + value);
            return 1;
        }

        String pong = connection.sync().ping();
        if (!"PONG".equals(pong)) {
            System.err.println("connection: expected PONG, got " + pong);
            return 1;
        }

        System.out.println("Lettuce works without Reactor");
        return 0;
    }

    private int requestReactiveApi() {
        try {
            connection.commands(RedisReactiveCommands.factory());
        } catch (UnsupportedOperationException e) {
            System.out.println(e.getMessage());
            return 0;
        }
        System.err.println("reactive API: expected an UnsupportedOperationException");
        return 1;
    }
}
