package io.quarkus.redis.lettuce.runtime.internal.set;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.ScanArgs;
import io.quarkus.redis.datasource.SortArgs;
import io.quarkus.redis.datasource.set.ReactiveSScanCursor;
import io.quarkus.redis.datasource.set.ReactiveSetCommands;
import io.quarkus.redis.lettuce.runtime.internal.AbstractLettuceCommands;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommonConverters;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveSetCommandsImpl<K, V> extends AbstractLettuceCommands<K, V>
        implements ReactiveSetCommands<K, V> {

    private static final SortArgs DEFAULT_SORT_ARGS = new SortArgs();

    private final ReactiveRedisDataSource dataSource;

    public LettuceReactiveSetCommandsImpl(ReactiveRedisDataSource dataSource,
            LettuceConnection connection, Type keyType, Type valueType) {
        super(connection, keyType, valueType, new Marshaller(keyType, valueType));
        this.dataSource = dataSource;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return dataSource;
    }

    @SafeVarargs
    @Override
    public final Uni<Integer> sadd(K key, V... values) {
        return _sadd(key, values).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Integer> _sadd(K key, V... values) {
        nonNull(key, "key");
        notNullOrEmpty(values, "members");
        doesNotContainNull(values, "members");
        return LettuceCommand.of(() -> async.sadd(marshaller.encode(key), marshaller.encodeAsArray(values)),
                AbstractLettuceCommands::toInteger);
    }

    @Override
    public Uni<Long> scard(K key) {
        return _scard(key).toUni();
    }

    LettuceCommand<Long, Long> _scard(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.scard(marshaller.encode(key)));
    }

    @SafeVarargs
    @Override
    public final Uni<Set<V>> sdiff(K... keys) {
        return _sdiff(keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Set<byte[]>, Set<V>> _sdiff(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return LettuceCommand.failing(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        return LettuceCommand.of(() -> async.sdiff(marshaller.encodeAsArray(keys)), this::decodeSetOfValues);
    }

    @SafeVarargs
    @Override
    public final Uni<Long> sdiffstore(K destination, K... keys) {
        return _sdiffstore(destination, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _sdiffstore(K destination, K... keys) {
        nonNull(destination, "destination");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return LettuceCommand.failing(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        return LettuceCommand.of(() -> async.sdiffstore(marshaller.encode(destination), marshaller.encodeAsArray(keys)));
    }

    @SafeVarargs
    @Override
    public final Uni<Set<V>> sinter(K... keys) {
        return _sinter(keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Set<byte[]>, Set<V>> _sinter(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return LettuceCommand.failing(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        return LettuceCommand.of(() -> async.sinter(marshaller.encodeAsArray(keys)), this::decodeSetOfValues);
    }

    @SafeVarargs
    @Override
    public final Uni<Long> sintercard(K... keys) {
        return _sintercard(keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _sintercard(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return LettuceCommand.failing(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        return LettuceCommand.of(() -> async.sintercard(marshaller.encodeAsArray(keys)));
    }

    @SafeVarargs
    @Override
    public final Uni<Long> sintercard(int limit, K... keys) {
        return _sintercard(limit, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _sintercard(int limit, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        positive(limit, "limit");
        if (keys.length < 2) {
            return LettuceCommand.failing(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        return LettuceCommand.of(() -> async.sintercard(limit, marshaller.encodeAsArray(keys)));
    }

    @SafeVarargs
    @Override
    public final Uni<Long> sinterstore(K destination, K... keys) {
        return _sinterstore(destination, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _sinterstore(K destination, K... keys) {
        nonNull(destination, "destination");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return LettuceCommand.failing(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        return LettuceCommand.of(() -> async.sinterstore(marshaller.encode(destination), marshaller.encodeAsArray(keys)));
    }

    @Override
    public Uni<Boolean> sismember(K key, V member) {
        return _sismember(key, member).toUni();
    }

    LettuceCommand<Boolean, Boolean> _sismember(K key, V member) {
        nonNull(key, "key");
        nonNull(member, "member");
        return LettuceCommand.of(() -> async.sismember(marshaller.encode(key), marshaller.encode(member)));
    }

    @Override
    public Uni<Set<V>> smembers(K key) {
        return _smembers(key).toUni();
    }

    LettuceCommand<Set<byte[]>, Set<V>> _smembers(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.smembers(marshaller.encode(key)), this::decodeSetOfValues);
    }

    @SafeVarargs
    @Override
    public final Uni<List<Boolean>> smismember(K key, V... members) {
        return _smismember(key, members).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<Boolean>, List<Boolean>> _smismember(K key, V... members) {
        nonNull(key, "key");
        notNullOrEmpty(members, "members");
        doesNotContainNull(members, "members");
        return LettuceCommand.of(() -> async.smismember(marshaller.encode(key), marshaller.encodeAsArray(members)),
                AbstractLettuceCommands::decodeList);
    }

    @Override
    public Uni<Boolean> smove(K source, K destination, V member) {
        return _smove(source, destination, member).toUni();
    }

    LettuceCommand<Boolean, Boolean> _smove(K source, K destination, V member) {
        nonNull(source, "source");
        nonNull(destination, "destination");
        nonNull(member, "member");
        return LettuceCommand.of(
                () -> async.smove(marshaller.encode(source), marshaller.encode(destination), marshaller.encode(member)));
    }

    @Override
    public Uni<V> spop(K key) {
        return _spop(key).toUni();
    }

    LettuceCommand<byte[], V> _spop(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.spop(marshaller.encode(key)), this::decodeValue);
    }

    @Override
    public Uni<Set<V>> spop(K key, int count) {
        return _spop(key, count).toUni();
    }

    LettuceCommand<Set<byte[]>, Set<V>> _spop(K key, int count) {
        nonNull(key, "key");
        positive(count, "count");
        return LettuceCommand.of(() -> async.spop(marshaller.encode(key), count), this::decodeSetOfValues);
    }

    @Override
    public Uni<V> srandmember(K key) {
        return _srandmember(key).toUni();
    }

    LettuceCommand<byte[], V> _srandmember(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.srandmember(marshaller.encode(key)), this::decodeValue);
    }

    @Override
    public Uni<List<V>> srandmember(K key, int count) {
        return _srandmember(key, count).toUni();
    }

    LettuceCommand<List<byte[]>, List<V>> _srandmember(K key, int count) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.srandmember(marshaller.encode(key), count), this::decodeListOfValues);
    }

    @SafeVarargs
    @Override
    public final Uni<Integer> srem(K key, V... members) {
        return _srem(key, members).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Integer> _srem(K key, V... members) {
        nonNull(key, "key");
        notNullOrEmpty(members, "members");
        doesNotContainNull(members, "members");
        return LettuceCommand.of(() -> async.srem(marshaller.encode(key), marshaller.encodeAsArray(members)),
                AbstractLettuceCommands::toInteger);
    }

    @SafeVarargs
    @Override
    public final Uni<Set<V>> sunion(K... keys) {
        return _sunion(keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Set<byte[]>, Set<V>> _sunion(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return LettuceCommand.failing(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        return LettuceCommand.of(() -> async.sunion(marshaller.encodeAsArray(keys)), this::decodeSetOfValues);
    }

    @SafeVarargs
    @Override
    public final Uni<Long> sunionstore(K destination, K... keys) {
        return _sunionstore(destination, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _sunionstore(K destination, K... keys) {
        nonNull(destination, "destination");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        if (keys.length < 2) {
            return LettuceCommand.failing(new IllegalArgumentException("`keys` must contain at least 2 keys"));
        }
        return LettuceCommand.of(() -> async.sunionstore(marshaller.encode(destination), marshaller.encodeAsArray(keys)));
    }

    @Override
    public ReactiveSScanCursor<V> sscan(K key) {
        nonNull(key, "key");
        return new LettuceReactiveSScanCursorImpl<>(async, marshaller.encode(key), this::decodeListOfValues);
    }

    @Override
    public ReactiveSScanCursor<V> sscan(K key, ScanArgs scanArgs) {
        nonNull(key, "key");
        nonNull(scanArgs, "scanArgs");
        return new LettuceReactiveSScanCursorImpl<>(async, marshaller.encode(key),
                LettuceCommonConverters.toLettuceScanArgs(scanArgs), this::decodeListOfValues);
    }

    @Override
    public Uni<List<V>> sort(K key) {
        return sort(key, DEFAULT_SORT_ARGS);
    }

    @Override
    public Uni<List<V>> sort(K key, SortArgs sortArguments) {
        return _sort(key, sortArguments).toUni();
    }

    LettuceCommand<List<byte[]>, List<V>> _sort(K key, SortArgs sortArguments) {
        nonNull(key, "key");
        nonNull(sortArguments, "sortArguments");
        io.lettuce.core.SortArgs lettuceArgs = LettuceCommonConverters.toLettuceSortArgs(sortArguments);
        return LettuceCommand.of(() -> async.sort(marshaller.encode(key), lettuceArgs), this::decodeListOfValues);
    }

    @Override
    public Uni<Long> sortAndStore(K key, K destination, SortArgs sortArguments) {
        return _sortAndStore(key, destination, sortArguments).toUni();
    }

    LettuceCommand<Long, Long> _sortAndStore(K key, K destination, SortArgs args) {
        nonNull(key, "key");
        nonNull(destination, "destination");
        nonNull(args, "args");
        io.lettuce.core.SortArgs lettuceArgs = LettuceCommonConverters.toLettuceSortArgs(args);
        return LettuceCommand.of(() -> async.sortStore(marshaller.encode(key), lettuceArgs, marshaller.encode(destination)));
    }

    @Override
    public Uni<Long> sortAndStore(K key, K destination) {
        return sortAndStore(key, destination, DEFAULT_SORT_ARGS);
    }

}
