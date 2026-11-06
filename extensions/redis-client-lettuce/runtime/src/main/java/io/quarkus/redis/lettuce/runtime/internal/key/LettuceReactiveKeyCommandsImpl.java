package io.quarkus.redis.lettuce.runtime.internal.key;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.quarkus.redis.runtime.datasource.Validation.positive;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positiveOrZero;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.CopyArgs;
import io.quarkus.redis.datasource.keys.ExpireArgs;
import io.quarkus.redis.datasource.keys.KeyScanArgs;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.keys.ReactiveKeyScanCursor;
import io.quarkus.redis.datasource.keys.RedisKeyNotFoundException;
import io.quarkus.redis.datasource.keys.RedisValueType;
import io.quarkus.redis.lettuce.runtime.internal.AbstractLettuceCommands;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveKeyCommandsImpl<K> extends AbstractLettuceCommands<K, K>
        implements ReactiveKeyCommands<K> {

    private final ReactiveRedisDataSource dataSource;

    public LettuceReactiveKeyCommandsImpl(ReactiveRedisDataSource dataSource,
            LettuceConnection connection, Type keyType) {
        super(connection, keyType, keyType, new Marshaller(keyType));
        this.dataSource = dataSource;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Boolean> copy(K source, K destination) {
        return _copy(source, destination).toUni();
    }

    LettuceCommand<Boolean, Boolean> _copy(K source, K destination) {
        nonNull(source, "source");
        nonNull(destination, "destination");
        return LettuceCommand.of(() -> async.copy(marshaller.encode(source), marshaller.encode(destination)));
    }

    @Override
    public Uni<Boolean> copy(K source, K destination, CopyArgs copyArgs) {
        return _copy(source, destination, copyArgs).toUni();
    }

    LettuceCommand<Boolean, Boolean> _copy(K source, K destination, CopyArgs copyArgs) {
        nonNull(source, "source");
        nonNull(destination, "destination");
        nonNull(copyArgs, "copyArgs");
        io.lettuce.core.CopyArgs lettuceArgs = LettuceKeyCommandsConverters.toLettuceCopyArgs(copyArgs);
        return LettuceCommand.of(() -> async.copy(marshaller.encode(source), marshaller.encode(destination), lettuceArgs));
    }

    @SafeVarargs
    @Override
    public final Uni<Integer> del(K... keys) {
        return _del(keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Integer> _del(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        return LettuceCommand.of(() -> async.del(marshaller.encodeAsArray(keys)), AbstractLettuceCommands::toInteger);
    }

    @Override
    public Uni<String> dump(K key) {
        return _dump(key).toUni();
    }

    LettuceCommand<byte[], String> _dump(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.dump(marshaller.encode(key)), this::decodeString);
    }

    @Override
    public Uni<Boolean> exists(K key) {
        return _exists(key).toUni();
    }

    LettuceCommand<Long, Boolean> _exists(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.exists(marshaller.encode(key)), c -> c != null && c > 0);
    }

    @SafeVarargs
    @Override
    public final Uni<Integer> exists(K... keys) {
        return _exists(keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Integer> _exists(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        return LettuceCommand.of(() -> async.exists(marshaller.encodeAsArray(keys)), AbstractLettuceCommands::toInteger);
    }

    @Override
    public Uni<Boolean> expire(K key, long seconds, ExpireArgs expireArgs) {
        return _expire(key, seconds, expireArgs).toUni();
    }

    LettuceCommand<Boolean, Boolean> _expire(K key, long seconds, ExpireArgs expireArgs) {
        nonNull(key, "key");
        positive(seconds, "seconds");
        nonNull(expireArgs, "expireArgs");
        io.lettuce.core.ExpireArgs lettuceArgs = LettuceKeyCommandsConverters.toLettuceExpireArgs(expireArgs);
        return LettuceCommand.of(() -> async.expire(marshaller.encode(key), seconds, lettuceArgs));
    }

    @Override
    public Uni<Boolean> expire(K key, Duration duration, ExpireArgs expireArgs) {
        return _expire(key, duration, expireArgs).toUni();
    }

    LettuceCommand<Boolean, Boolean> _expire(K key, Duration duration, ExpireArgs expireArgs) {
        nonNull(duration, "duration");
        return _expire(key, duration.toSeconds(), expireArgs);
    }

    @Override
    public Uni<Boolean> expire(K key, long seconds) {
        return _expire(key, seconds).toUni();
    }

    LettuceCommand<Boolean, Boolean> _expire(K key, long seconds) {
        return _expire(key, seconds, new ExpireArgs());
    }

    @Override
    public Uni<Boolean> expire(K key, Duration duration) {
        return _expire(key, duration).toUni();
    }

    LettuceCommand<Boolean, Boolean> _expire(K key, Duration duration) {
        nonNull(duration, "duration");
        return _expire(key, duration.toSeconds());
    }

    @Override
    public Uni<Boolean> expireat(K key, long timestamp) {
        return _expireat(key, timestamp).toUni();
    }

    LettuceCommand<Boolean, Boolean> _expireat(K key, long timestamp) {
        nonNull(key, "key");
        positive(timestamp, "timestamp");
        return LettuceCommand.of(() -> async.expireat(marshaller.encode(key), timestamp));
    }

    @Override
    public Uni<Boolean> expireat(K key, Instant timestamp) {
        return _expireat(key, timestamp).toUni();
    }

    LettuceCommand<Boolean, Boolean> _expireat(K key, Instant timestamp) {
        nonNull(timestamp, "timestamp");
        return _expireat(key, timestamp.getEpochSecond());
    }

    @Override
    public Uni<Boolean> expireat(K key, long timestamp, ExpireArgs expireArgs) {
        return _expireat(key, timestamp, expireArgs).toUni();
    }

    LettuceCommand<Boolean, Boolean> _expireat(K key, long timestamp, ExpireArgs expireArgs) {
        nonNull(key, "key");
        positive(timestamp, "timestamp");
        nonNull(expireArgs, "expireArgs");
        io.lettuce.core.ExpireArgs lettuceArgs = LettuceKeyCommandsConverters.toLettuceExpireArgs(expireArgs);
        return LettuceCommand.of(() -> async.expireat(marshaller.encode(key), timestamp, lettuceArgs));
    }

    @Override
    public Uni<Boolean> expireat(K key, Instant timestamp, ExpireArgs expireArgs) {
        return _expireat(key, timestamp, expireArgs).toUni();
    }

    LettuceCommand<Boolean, Boolean> _expireat(K key, Instant timestamp, ExpireArgs expireArgs) {
        nonNull(timestamp, "timestamp");
        return _expireat(key, timestamp.getEpochSecond(), expireArgs);
    }

    @Override
    public Uni<Long> expiretime(K key) {
        return _expiretime(key).toUni();
    }

    LettuceCommand<Long, Long> _expiretime(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.expiretime(marshaller.encode(key)), r -> decodeExpireResponse(key, r));
    }

    @Override
    public Uni<List<K>> keys(String pattern) {
        return _keys(pattern).toUni();
    }

    LettuceCommand<List<byte[]>, List<K>> _keys(String pattern) {
        nonNull(pattern, "pattern");
        if (pattern.isBlank()) {
            throw new IllegalArgumentException("`pattern` must not be blank");
        }
        return LettuceCommand.of(() -> async.keys(pattern), this::decodeListOfKeys);
    }

    @Override
    public Uni<Boolean> move(K key, long db) {
        return _move(key, db).toUni();
    }

    LettuceCommand<Boolean, Boolean> _move(K key, long db) {
        nonNull(key, "key");
        positiveOrZero(db, "db");
        if (db > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("`db` must fit in a positive int");
        }
        return LettuceCommand.of(() -> async.move(marshaller.encode(key), (int) db));
    }

    @Override
    public Uni<Boolean> persist(K key) {
        return _persist(key).toUni();
    }

    LettuceCommand<Boolean, Boolean> _persist(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.persist(marshaller.encode(key)));
    }

    @Override
    public Uni<Boolean> pexpire(K key, long milliseconds, ExpireArgs expireArgs) {
        return _pexpire(key, milliseconds, expireArgs).toUni();
    }

    LettuceCommand<Boolean, Boolean> _pexpire(K key, long milliseconds, ExpireArgs expireArgs) {
        nonNull(key, "key");
        positive(milliseconds, "milliseconds");
        nonNull(expireArgs, "expireArgs");
        io.lettuce.core.ExpireArgs lettuceArgs = LettuceKeyCommandsConverters.toLettuceExpireArgs(expireArgs);
        return LettuceCommand.of(() -> async.pexpire(marshaller.encode(key), milliseconds, lettuceArgs));
    }

    @Override
    public Uni<Boolean> pexpire(K key, Duration duration, ExpireArgs expireArgs) {
        return _pexpire(key, duration, expireArgs).toUni();
    }

    LettuceCommand<Boolean, Boolean> _pexpire(K key, Duration duration, ExpireArgs expireArgs) {
        nonNull(duration, "duration");
        return _pexpire(key, duration.toMillis(), expireArgs);
    }

    @Override
    public Uni<Boolean> pexpire(K key, long ms) {
        return _pexpire(key, ms).toUni();
    }

    LettuceCommand<Boolean, Boolean> _pexpire(K key, long ms) {
        return _pexpire(key, ms, new ExpireArgs());
    }

    @Override
    public Uni<Boolean> pexpire(K key, Duration duration) {
        return _pexpire(key, duration).toUni();
    }

    LettuceCommand<Boolean, Boolean> _pexpire(K key, Duration duration) {
        nonNull(duration, "duration");
        return _pexpire(key, duration.toMillis());
    }

    @Override
    public Uni<Boolean> pexpireat(K key, long timestamp) {
        return _pexpireat(key, timestamp).toUni();
    }

    LettuceCommand<Boolean, Boolean> _pexpireat(K key, long timestamp) {
        nonNull(key, "key");
        positive(timestamp, "timestamp");
        return LettuceCommand.of(() -> async.pexpireat(marshaller.encode(key), timestamp));
    }

    @Override
    public Uni<Boolean> pexpireat(K key, Instant timestamp) {
        return _pexpireat(key, timestamp).toUni();
    }

    LettuceCommand<Boolean, Boolean> _pexpireat(K key, Instant timestamp) {
        nonNull(timestamp, "timestamp");
        return _pexpireat(key, timestamp.toEpochMilli());
    }

    @Override
    public Uni<Boolean> pexpireat(K key, long timestamp, ExpireArgs expireArgs) {
        return _pexpireat(key, timestamp, expireArgs).toUni();
    }

    LettuceCommand<Boolean, Boolean> _pexpireat(K key, long timestamp, ExpireArgs expireArgs) {
        nonNull(key, "key");
        positive(timestamp, "timestamp");
        nonNull(expireArgs, "expireArgs");
        io.lettuce.core.ExpireArgs lettuceArgs = LettuceKeyCommandsConverters.toLettuceExpireArgs(expireArgs);
        return LettuceCommand.of(() -> async.pexpireat(marshaller.encode(key), timestamp, lettuceArgs));
    }

    @Override
    public Uni<Boolean> pexpireat(K key, Instant timestamp, ExpireArgs expireArgs) {
        return _pexpireat(key, timestamp, expireArgs).toUni();
    }

    LettuceCommand<Boolean, Boolean> _pexpireat(K key, Instant timestamp, ExpireArgs expireArgs) {
        nonNull(timestamp, "timestamp");
        return _pexpireat(key, timestamp.toEpochMilli(), expireArgs);
    }

    @Override
    public Uni<Long> pexpiretime(K key) {
        return _pexpiretime(key).toUni();
    }

    LettuceCommand<Long, Long> _pexpiretime(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.pexpiretime(marshaller.encode(key)), r -> decodeExpireResponse(key, r));
    }

    @Override
    public Uni<Long> pttl(K key) {
        return _pttl(key).toUni();
    }

    LettuceCommand<Long, Long> _pttl(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.pttl(marshaller.encode(key)), r -> decodeExpireResponse(key, r));
    }

    @Override
    public Uni<K> randomkey() {
        return _randomkey().toUni();
    }

    LettuceCommand<byte[], K> _randomkey() {
        return LettuceCommand.of(async::randomkey, this::decodeKey);
    }

    @Override
    public Uni<Void> rename(K key, K newkey) {
        // Failure mapping is reactive-only: inside a transaction the error surfaces as the entry itself.
        return _rename(key, newkey).toUni()
                .onFailure().transform(t -> mapNoSuchKey(key, t));
    }

    LettuceCommand<String, Void> _rename(K key, K newkey) {
        nonNull(key, "key");
        nonNull(newkey, "newkey");
        return LettuceCommand.discarding(() -> async.rename(marshaller.encode(key), marshaller.encode(newkey)));
    }

    @Override
    public Uni<Boolean> renamenx(K key, K newkey) {
        // Failure mapping is reactive-only: inside a transaction the error surfaces as the entry itself.
        return _renamenx(key, newkey).toUni()
                .onFailure().transform(t -> mapNoSuchKey(key, t));
    }

    LettuceCommand<Boolean, Boolean> _renamenx(K key, K newkey) {
        nonNull(key, "key");
        nonNull(newkey, "newkey");
        return LettuceCommand.of(() -> async.renamenx(marshaller.encode(key), marshaller.encode(newkey)));
    }

    private Throwable mapNoSuchKey(K key, Throwable t) {
        String msg = t.getMessage();
        if (msg != null && msg.toLowerCase().contains("no such key")) {
            return new NoSuchElementException(String.valueOf(key));
        }
        return t;
    }

    @Override
    public ReactiveKeyScanCursor<K> scan() {
        return new LettuceReactiveKeyScanCursorImpl<>(async, this::decodeListOfKeys);
    }

    @Override
    public ReactiveKeyScanCursor<K> scan(KeyScanArgs args) {
        nonNull(args, "args");
        return new LettuceReactiveKeyScanCursorImpl<>(async, LettuceKeyCommandsConverters.toLettuceKeyScanArgs(args),
                this::decodeListOfKeys);
    }

    @SafeVarargs
    @Override
    public final Uni<Integer> touch(K... keys) {
        return _touch(keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Integer> _touch(K... keys) {
        notNullOrEmpty(keys, "keys");
        return LettuceCommand.of(() -> async.touch(marshaller.encodeAsArray(keys)), AbstractLettuceCommands::toInteger);
    }

    @Override
    public Uni<Long> ttl(K key) {
        return _ttl(key).toUni();
    }

    LettuceCommand<Long, Long> _ttl(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.ttl(marshaller.encode(key)), r -> decodeExpireResponse(key, r));
    }

    @Override
    public Uni<RedisValueType> type(K key) {
        return _type(key).toUni();
    }

    LettuceCommand<String, RedisValueType> _type(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.type(marshaller.encode(key)),
                s -> s == null ? null : RedisValueType.valueOf(s.toUpperCase()));
    }

    @SafeVarargs
    @Override
    public final Uni<Integer> unlink(K... keys) {
        return _unlink(keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Integer> _unlink(K... keys) {
        notNullOrEmpty(keys, "keys");
        return LettuceCommand.of(() -> async.unlink(marshaller.encodeAsArray(keys)), AbstractLettuceCommands::toInteger);
    }

    private long decodeExpireResponse(K key, Long r) {
        if (r == null || r == -2L) {
            throw new RedisKeyNotFoundException(String.valueOf(key));
        }
        return r;
    }

}
