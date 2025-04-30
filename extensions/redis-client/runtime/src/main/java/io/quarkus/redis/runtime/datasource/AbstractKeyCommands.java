package io.quarkus.redis.runtime.datasource;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;
import static io.smallrye.mutiny.helpers.ParameterValidation.positiveOrZero;
import static io.vertx.mutiny.redis.client.Command.EXPIRETIME;
import static io.vertx.mutiny.redis.client.Command.PEXPIRETIME;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import io.quarkus.redis.datasource.keys.CopyArgs;
import io.quarkus.redis.datasource.keys.ExpireArgs;
import io.quarkus.redis.datasource.keys.RedisKeyNotFoundException;
import io.quarkus.redis.datasource.keys.RedisValueType;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

class AbstractKeyCommands<K> extends AbstractRedisCommands {

    protected final Type typeOfKey;

    AbstractKeyCommands(RedisCommandExecutor redis, Type k) {
        super(redis, new Marshaller(k));
        this.typeOfKey = k;
    }

    Uni<Response> _copy(K source, K destination) {
        nonNull(source, "source");
        nonNull(destination, "destination");
        return execute(RedisCommand.of(Command.COPY)
                .put(marshaller.encode(source)).put(marshaller.encode(destination)));
    }

    Uni<Response> _copy(K source, K destination, CopyArgs copyArgs) {
        nonNull(source, "source");
        nonNull(destination, "destination");
        nonNull(copyArgs, "copyArgs");
        RedisCommand cmd = RedisCommand.of(Command.COPY);
        cmd.put(marshaller.encode(source));
        cmd.put(marshaller.encode(destination));
        cmd.putAll(copyArgs.toArgs());
        return execute(cmd);
    }

    Uni<Response> _del(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        RedisCommand cmd = RedisCommand.of(Command.DEL);
        for (K key : keys) {
            cmd.put(marshaller.encode(key));
        }
        return execute(cmd);
    }

    Uni<Response> _dump(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.DUMP).put(marshaller.encode(key)));
    }

    String decodeStringOrNull(Response r) {
        if (r == null) {
            return null;
        }
        return r.toString();
    }

    Uni<Response> _exists(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.EXISTS).put(marshaller.encode(key)));
    }

    Uni<Response> _exists(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        return execute(RedisCommand.of(Command.EXISTS).put(marshaller.encode(keys)));
    }

    Uni<Response> _expire(K key, long seconds, ExpireArgs expireArgs) {
        nonNull(key, "key");
        positive(seconds, "seconds");
        nonNull(expireArgs, "expireArgs");
        RedisCommand cmd = RedisCommand.of(Command.EXPIRE);
        cmd.put(marshaller.encode(key));
        cmd.put(seconds);
        cmd.putArgs(expireArgs);
        return execute(cmd);
    }

    Uni<Response> _expire(K key, Duration duration, ExpireArgs expireArgs) {
        return _expire(key, duration.toSeconds(), expireArgs);
    }

    Uni<Response> _expire(K key, long seconds) {
        return _expire(key, seconds, new ExpireArgs());
    }

    Uni<Response> _expire(K key, Duration duration) {
        return _expire(key, duration.toSeconds(), new ExpireArgs());
    }

    Uni<Response> _expireat(K key, long timestamp) {
        return _expireat(key, timestamp, new ExpireArgs());
    }

    Uni<Response> _expireat(K key, Instant timestamp) {
        return _expireat(key, timestamp.getEpochSecond(), new ExpireArgs());
    }

    Uni<Response> _expireat(K key, long timestamp, ExpireArgs expireArgs) {
        nonNull(key, "key");
        positive(timestamp, "timestamp");
        nonNull(expireArgs, "expireArgs");
        RedisCommand cmd = RedisCommand.of(Command.EXPIREAT);
        cmd.put(marshaller.encode(key));
        cmd.put(timestamp);
        cmd.putArgs(expireArgs);
        return execute(cmd);
    }

    Uni<Response> _expireat(K key, Instant timestamp, ExpireArgs expireArgs) {
        return _expireat(key, timestamp.getEpochSecond(), expireArgs);
    }

    Uni<Response> _expiretime(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(EXPIRETIME).put(marshaller.encode(key)));
    }

    long decodeExpireResponse(K key, Response r) {
        long res = r.toLong();
        if (res == -2) {
            throw new RedisKeyNotFoundException(new String(marshaller.encode(key), StandardCharsets.UTF_8));
        }
        return res;
    }

    Uni<Response> _keys(String pattern) {
        nonNull(pattern, "pattern");
        if (pattern.isBlank()) {
            return Uni.createFrom().failure(new IllegalArgumentException("`pattern` must not be blank"));
        }

        return execute(RedisCommand.of(Command.KEYS).put(pattern));
    }

    List<K> decodeKeys(Response response) {
        return marshaller.decodeAsList(response, typeOfKey);
    }

    Uni<Response> _move(K key, long db) {
        nonNull(key, "key");
        positiveOrZero(db, "db");
        return execute(RedisCommand.of(Command.MOVE).put(marshaller.encode(key)).put(db));
    }

    Uni<Response> _persist(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.PERSIST).put(marshaller.encode(key)));
    }

    Uni<Response> _pexpire(K key, long milliseconds, ExpireArgs expireArgs) {
        nonNull(key, "key");
        positive(milliseconds, "milliseconds");
        nonNull(expireArgs, "expireArgs");
        RedisCommand cmd = RedisCommand.of(Command.PEXPIRE);
        cmd.put(marshaller.encode(key));
        cmd.put(Long.toString(milliseconds));
        cmd.put(expireArgs);
        return execute(cmd);
    }

    Uni<Response> _pexpire(K key, Duration duration, ExpireArgs expireArgs) {
        return _pexpire(key, duration.toMillis(), expireArgs);
    }

    Uni<Response> _pexpire(K key, long ms) {
        return _pexpire(key, ms, new ExpireArgs());
    }

    Uni<Response> _pexpire(K key, Duration duration) {
        return _pexpire(key, duration.toMillis(), new ExpireArgs());
    }

    Uni<Response> _pexpireat(K key, long timestamp) {
        return _pexpireat(key, timestamp, new ExpireArgs());
    }

    Uni<Response> _pexpireat(K key, Instant timestamp) {
        return _pexpireat(key, timestamp.toEpochMilli(), new ExpireArgs());
    }

    Uni<Response> _pexpireat(K key, long timestamp, ExpireArgs expireArgs) {
        nonNull(key, "key");
        positive(timestamp, "timestamp");
        nonNull(expireArgs, "expireArgs");
        RedisCommand cmd = RedisCommand.of(Command.PEXPIREAT);
        cmd.put(marshaller.encode(key));
        cmd.put(Long.toString(timestamp));
        cmd.put(expireArgs.toArgs());
        return execute(cmd);
    }

    Uni<Response> _pexpireat(K key, Instant timestamp, ExpireArgs expireArgs) {
        return _pexpireat(key, timestamp.toEpochMilli(), expireArgs);
    }

    Uni<Response> _pexpiretime(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(PEXPIRETIME).put(marshaller.encode(key)));
    }

    Uni<Response> _pttl(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.PTTL).put(marshaller.encode(key)));
    }

    Uni<Response> _randomkey() {
        return execute(RedisCommand.of(Command.RANDOMKEY));
    }

    K decodeK(Response r) {
        return marshaller.decode(typeOfKey, r);
    }

    Uni<Response> _rename(K key, K newKey) {
        nonNull(key, "key");
        nonNull(newKey, "newKey");
        return execute(RedisCommand.of(Command.RENAME)
                .put(marshaller.encode(key)).put(marshaller.encode(newKey)))
                .onFailure().transform(t -> {
                    if (t.getMessage().equalsIgnoreCase("ERR no such key")) {
                        return new NoSuchElementException(new String(marshaller.encode(key), StandardCharsets.UTF_8));
                    }
                    return t;
                });
    }

    Uni<Response> _renamenx(K key, K newKey) {
        nonNull(key, "key");
        nonNull(newKey, "newKey");
        return execute(RedisCommand.of(Command.RENAMENX)
                .put(marshaller.encode(key)).put(marshaller.encode(newKey)))
                .onFailure().transform(t -> {
                    if (t.getMessage().equalsIgnoreCase("ERR no such key")) {
                        return new NoSuchElementException(new String(marshaller.encode(key), StandardCharsets.UTF_8));
                    }
                    return t;
                });
    }

    Uni<Response> _touch(K... keys) {
        notNullOrEmpty(keys, "keys");
        RedisCommand cmd = RedisCommand.of(Command.TOUCH);
        for (K key : keys) {
            cmd.put(marshaller.encode(key));
        }
        return execute(cmd);
    }

    Uni<Response> _ttl(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.TTL).put(marshaller.encode(key)));
    }

    Uni<Response> _type(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.TYPE).put(marshaller.encode(key)));
    }

    RedisValueType decodeRedisType(Response r) {
        return RedisValueType.valueOf(r.toString().toUpperCase());
    }

    Uni<Response> _unlink(K... keys) {
        notNullOrEmpty(keys, "keys");
        RedisCommand cmd = RedisCommand.of(Command.UNLINK);
        for (K key : keys) {
            cmd.put(marshaller.encode(key));
        }
        return execute(cmd);
    }

}
