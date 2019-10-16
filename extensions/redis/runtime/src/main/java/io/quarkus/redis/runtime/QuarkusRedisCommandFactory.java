package io.quarkus.redis.runtime;

import javax.enterprise.context.ApplicationScoped;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.dynamic.Commands;
import io.lettuce.core.dynamic.RedisCommandFactory;

@ApplicationScoped
public class QuarkusRedisCommandFactory {
    RedisCommandFactory factory;

    public QuarkusRedisCommandFactory() {

    }

    void initialize(StatefulConnection<String, String> connection) {
        factory = new RedisCommandFactory(connection);
    }

    public Commands create(Class<? extends Commands> commandClass) {
        return factory.getCommands(commandClass);
    }
}
