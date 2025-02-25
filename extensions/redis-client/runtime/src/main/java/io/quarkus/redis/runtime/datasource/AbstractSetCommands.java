package io.quarkus.redis.runtime.datasource;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

class AbstractSetCommands<K, V> extends ReactiveSortable<K, V> {

    protected final Type typeOfValue;

    AbstractSetCommands(RedisCommandExecutor redis, Type k, Type v) {
        super(redis, new Marshaller(k, v), v);
        this.typeOfValue = v;
    }

    Uni<Response> _sadd(K key, V... members) {
        nonNull(key, "key");
        notNullOrEmpty(members, "members");
        RedisCommand cmd = RedisCommand.of(Command.SADD);
        cmd.put(marshaller.encode(key));
        cmd.putAll(marshaller.encode(members));
        return execute(cmd);
    }

    Uni<Response> _scard(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.SCARD).put(marshaller.encode(key)));
    }

    Uni<Response> _sdiff(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length <= 1) {
            return Uni.createFrom().failure(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        return execute(RedisCommand.of(Command.SDIFF).put(marshaller.encode(keys)));
    }

    Set<V> decodeAsSetOfValues(Response r) {
        return marshaller.decodeAsSet(r, typeOfValue);
    }

    Uni<Response> _sdiffstore(K destination, K... keys) {
        nonNull(destination, "destination");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length <= 1) {
            return Uni.createFrom().failure(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        RedisCommand cmd = RedisCommand.of(Command.SDIFFSTORE)
                .put(marshaller.encode(destination))
                .putAll(marshaller.encode(keys));
        return execute(cmd);
    }

    Uni<Response> _sinter(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length <= 1) {
            return Uni.createFrom().failure(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        return execute(RedisCommand.of(Command.SINTER).put(marshaller.encode(keys)));
    }

    Uni<Response> _sintercard(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length <= 1) {
            return Uni.createFrom().failure(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }

        RedisCommand cmd = RedisCommand.of(Command.SINTERCARD).put(keys.length).putAll(marshaller.encode(keys));
        return execute(cmd);
    }

    //TODO To be tested
    Uni<Response> _sintercard(int limit, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        positive(limit, "limit");

        if (keys.length <= 1) {
            return Uni.createFrom().failure(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }

        RedisCommand cmd = RedisCommand.of(Command.SINTERCARD).put(keys.length).putAll(marshaller.encode(keys))
                .put("LIMIT").put(limit);
        return execute(cmd);
    }

    Uni<Response> _sinterstore(K destination, K... keys) {
        nonNull(destination, "destination");
        notNullOrEmpty(keys, "keys");
        if (keys.length <= 1) {
            return Uni.createFrom().failure(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        RedisCommand cmd = RedisCommand.of(Command.SINTERSTORE)
                .put(marshaller.encode(destination))
                .putAll(marshaller.encode(keys));
        return execute(cmd);
    }

    Uni<Response> _sismember(K key, V member) {
        nonNull(key, "key");
        nonNull(member, "member");
        return execute(RedisCommand.of(Command.SISMEMBER).put(marshaller.encode(key)).put(marshaller.encode(member)));
    }

    Uni<Response> _smembers(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.SMEMBERS).put(marshaller.encode(key)));
    }

    Uni<Response> _smismember(K key, V... members) {
        nonNull(key, "key");
        notNullOrEmpty(members, "members");
        RedisCommand cmd = RedisCommand.of(Command.SMISMEMBER);
        cmd.put(marshaller.encode(key));
        cmd.putAll(marshaller.encode(members));
        return execute(cmd);
    }

    List<Boolean> decodeAsListOfBooleans(Response r) {
        return marshaller.decodeAsList(r, Response::toBoolean);
    }

    Uni<Response> _smove(K source, K destination, V member) {
        nonNull(source, "source");
        nonNull(destination, "destination");
        nonNull(member, "member");
        return execute(RedisCommand.of(Command.SMOVE)
                .put(marshaller.encode(source))
                .put(marshaller.encode(destination))
                .put(marshaller.encode(member)));
    }

    Uni<Response> _spop(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.SPOP).put(marshaller.encode(key)));
    }

    V decodeV(Response r) {
        return marshaller.decode(typeOfValue, r);
    }

    Uni<Response> _spop(K key, int count) {
        nonNull(key, "key");
        positive(count, "count");
        return execute(RedisCommand.of(Command.SPOP).put(marshaller.encode(key)).put(count));
    }

    Uni<Response> _srandmember(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.SRANDMEMBER).put(marshaller.encode(key)));
    }

    Uni<Response> _srandmember(K key, int count) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.SRANDMEMBER).put(marshaller.encode(key)).put(count));
    }

    List<V> decodeListOfValues(Response r) {
        return marshaller.decodeAsList(r, typeOfValue);
    }

    Uni<Response> _srem(K key, V... members) {
        nonNull(key, "key");
        notNullOrEmpty(members, "members");
        doesNotContainNull(members, "members");
        RedisCommand cmd = RedisCommand.of(Command.SREM);
        cmd.put(marshaller.encode(key));
        cmd.putAll(marshaller.encode(members));
        return execute(cmd);
    }

    Uni<Response> _sunion(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length <= 1) {
            return Uni.createFrom().failure(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        return execute(RedisCommand.of(Command.SUNION).put(marshaller.encode(keys)));
    }

    Uni<Response> _sunionstore(K destination, K... keys) {
        nonNull(destination, "destination");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length <= 1) {
            return Uni.createFrom().failure(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        RedisCommand cmd = RedisCommand.of(Command.SUNIONSTORE)
                .put(marshaller.encode(destination))
                .putAll(marshaller.encode(keys));
        return execute(cmd);
    }

}
