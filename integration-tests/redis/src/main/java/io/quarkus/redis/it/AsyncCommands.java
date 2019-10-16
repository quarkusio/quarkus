package io.quarkus.redis.it;

import java.util.concurrent.CompletableFuture;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.dynamic.Commands;

public interface AsyncCommands extends Commands {
    CompletableFuture<String> get(String key);

    RedisFuture<String> set(String key, String value);
}
