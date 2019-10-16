package io.quarkus.redis.it;

import io.lettuce.core.dynamic.Commands;
import io.lettuce.core.dynamic.annotation.Command;
import io.reactivex.Single;
import reactor.core.publisher.Mono;

public interface ReactiveCommands extends Commands {
    Mono<String> get(String key);

    @Command("GET")
    Single<String> single(String key);

    Mono<String> set(String key, String value);
}
