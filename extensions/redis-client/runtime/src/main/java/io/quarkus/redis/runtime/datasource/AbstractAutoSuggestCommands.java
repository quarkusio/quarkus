package io.quarkus.redis.runtime.datasource;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrBlank;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;

import io.quarkus.redis.datasource.autosuggest.GetArgs;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

public class AbstractAutoSuggestCommands<K> extends AbstractRedisCommands {

    AbstractAutoSuggestCommands(RedisCommandExecutor redis, Type k) {
        super(redis, new Marshaller(k));
    }

    Uni<Response> _ftSugAdd(K key, String string, double score, boolean increment) {
        nonNull(key, "key");
        notNullOrBlank(string, "string");
        RedisCommand cmd = RedisCommand.of(Command.FT_SUGADD).put(marshaller.encode(key)).put(string).put(score);
        if (increment) {
            cmd.put("INCR");
        }
        return execute(cmd);
    }

    Uni<Response> _ftSugDel(K key, String string) {
        nonNull(key, "key");
        notNullOrBlank(string, "string");
        RedisCommand cmd = RedisCommand.of(Command.FT_SUGDEL).put(marshaller.encode(key)).put(string);
        return execute(cmd);
    }

    Uni<Response> _ftSugget(K key, String prefix) {
        nonNull(key, "key");
        notNullOrBlank(prefix, "prefix");
        RedisCommand cmd = RedisCommand.of(Command.FT_SUGGET).put(marshaller.encode(key)).put(prefix);
        return execute(cmd);
    }

    Uni<Response> _ftSugget(K key, String prefix, GetArgs args) {
        nonNull(key, "key");
        notNullOrBlank(prefix, "prefix");
        nonNull(args, "args");
        RedisCommand cmd = RedisCommand.of(Command.FT_SUGGET).put(marshaller.encode(key)).put(prefix).putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _ftSugLen(K key) {
        nonNull(key, "key");
        RedisCommand cmd = RedisCommand.of(Command.FT_SUGLEN).put(marshaller.encode(key));
        return execute(cmd);
    }
}
