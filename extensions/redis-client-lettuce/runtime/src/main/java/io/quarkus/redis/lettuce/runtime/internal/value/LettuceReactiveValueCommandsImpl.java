package io.quarkus.redis.lettuce.runtime.internal.value;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;
import static io.smallrye.mutiny.helpers.ParameterValidation.positiveOrZero;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import io.lettuce.core.KeyValue;
import io.lettuce.core.LcsArgs;
import io.lettuce.core.StringMatchResult;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.string.ReactiveStringCommands;
import io.quarkus.redis.datasource.value.GetExArgs;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.lettuce.runtime.internal.AbstractLettuceCommands;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveValueCommandsImpl<K, V> extends AbstractLettuceCommands<K, V>
        implements ReactiveStringCommands<K, V>, ReactiveValueCommands<K, V> {

    private final ReactiveRedisDataSource dataSource;

    public LettuceReactiveValueCommandsImpl(ReactiveRedisDataSource dataSource,
            LettuceConnection connection, Type keyType, Type valueType) {
        super(connection, keyType, valueType, new Marshaller(keyType, valueType));
        this.dataSource = dataSource;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Long> append(K key, V value) {
        return _append(key, value).toUni();
    }

    LettuceCommand<Long, Long> _append(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return LettuceCommand.of(() -> async.append(marshaller.encode(key), marshaller.encode(value)));
    }

    @Override
    public Uni<Long> decr(K key) {
        return _decr(key).toUni();
    }

    LettuceCommand<Long, Long> _decr(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.decr(marshaller.encode(key)));
    }

    @Override
    public Uni<Long> decrby(K key, long amount) {
        return _decrby(key, amount).toUni();
    }

    LettuceCommand<Long, Long> _decrby(K key, long amount) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.decrby(marshaller.encode(key), amount));
    }

    @Override
    public Uni<V> get(K key) {
        return _get(key).toUni();
    }

    LettuceCommand<byte[], V> _get(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.get(marshaller.encode(key)), this::decodeValue);
    }

    @Override
    public Uni<V> getdel(K key) {
        return _getdel(key).toUni();
    }

    LettuceCommand<byte[], V> _getdel(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.getdel(marshaller.encode(key)), this::decodeValue);
    }

    @Override
    public Uni<V> getex(K key, GetExArgs args) {
        return _getex(key, args).toUni();
    }

    @Override
    public Uni<V> getex(K key, io.quarkus.redis.datasource.string.GetExArgs args) {
        return getex(key, (GetExArgs) args);
    }

    LettuceCommand<byte[], V> _getex(K key, GetExArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");
        io.lettuce.core.GetExArgs lettuceArgs = LettuceValueCommandsConverters.toLettuceGetExArgs(args);
        return LettuceCommand.of(() -> async.getex(marshaller.encode(key), lettuceArgs), this::decodeValue);
    }

    @Override
    public Uni<String> getrange(K key, long start, long end) {
        return _getrange(key, start, end).toUni();
    }

    LettuceCommand<byte[], String> _getrange(K key, long start, long end) {
        nonNull(key, "key");
        positiveOrZero(start, "start");
        return LettuceCommand.of(() -> async.getrange(marshaller.encode(key), start, end), this::decodeString);
    }

    @Deprecated
    @Override
    public Uni<V> getset(K key, V value) {
        return _getset(key, value).toUni();
    }

    LettuceCommand<byte[], V> _getset(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return LettuceCommand.of(() -> async.getset(marshaller.encode(key), marshaller.encode(value)), this::decodeValue);
    }

    @Override
    public Uni<Long> incr(K key) {
        return _incr(key).toUni();
    }

    LettuceCommand<Long, Long> _incr(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.incr(marshaller.encode(key)));
    }

    @Override
    public Uni<Long> incrby(K key, long amount) {
        return _incrby(key, amount).toUni();
    }

    LettuceCommand<Long, Long> _incrby(K key, long amount) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.incrby(marshaller.encode(key), amount));
    }

    @Override
    public Uni<Double> incrbyfloat(K key, double amount) {
        return _incrbyfloat(key, amount).toUni();
    }

    LettuceCommand<Double, Double> _incrbyfloat(K key, double amount) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.incrbyfloat(marshaller.encode(key), amount));
    }

    @Override
    public Uni<String> lcs(K key1, K key2) {
        return _lcs(key1, key2).toUni();
    }

    LettuceCommand<byte[], String> _lcs(K key1, K key2) {
        nonNull(key1, "key1");
        nonNull(key2, "key2");
        return LettuceCommand.of(() -> async.lcs(marshaller.encode(key1), marshaller.encode(key2)), this::decodeString);
    }

    @Override
    public Uni<Long> lcsLength(K key1, K key2) {
        return _lcsLength(key1, key2).toUni();
    }

    LettuceCommand<StringMatchResult, Long> _lcsLength(K key1, K key2) {
        nonNull(key1, "key1");
        nonNull(key2, "key2");
        LcsArgs args = LcsArgs.Builder.justLen();
        return LettuceCommand.of(() -> async.lcs(marshaller.encode(key1), marshaller.encode(key2), args),
                result -> result == null ? null : result.getLen());
    }

    @SafeVarargs
    @Override
    public final Uni<Map<K, V>> mget(K... keys) {
        return _mget(keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<KeyValue<byte[], byte[]>>, Map<K, V>> _mget(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        return LettuceCommand.of(() -> async.mget(marshaller.encodeAsArray(keys)), r -> decodeOrderedMap(keys, r));
    }

    @Override
    public Uni<Void> mset(Map<K, V> map) {
        return _mset(map).toUni();
    }

    LettuceCommand<String, Void> _mset(Map<K, V> map) {
        notNullOrEmpty(map, "map");
        return LettuceCommand.discarding(() -> async.mset(encodeMap(map)));
    }

    @Override
    public Uni<Boolean> msetnx(Map<K, V> map) {
        return _msetnx(map).toUni();
    }

    LettuceCommand<Boolean, Boolean> _msetnx(Map<K, V> map) {
        notNullOrEmpty(map, "map");
        return LettuceCommand.of(() -> async.msetnx(encodeMap(map)));
    }

    @Override
    public Uni<Void> psetex(K key, long milliseconds, V value) {
        return _psetex(key, milliseconds, value).toUni();
    }

    LettuceCommand<String, Void> _psetex(K key, long milliseconds, V value) {
        nonNull(key, "key");
        positive(milliseconds, "milliseconds");
        nonNull(value, "value");
        return LettuceCommand.discarding(() -> async.psetex(marshaller.encode(key), milliseconds, marshaller.encode(value)));
    }

    @Override
    public Uni<Void> set(K key, V value) {
        return _set(key, value).discarding().toUni();
    }

    LettuceCommand<String, Boolean> _set(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return LettuceCommand.of(() -> async.set(marshaller.encode(key), marshaller.encode(value)),
                AbstractLettuceCommands::isOk);
    }

    @Override
    public Uni<Void> set(K key, V value, SetArgs setArgs) {
        return _set(key, value, setArgs).discarding().toUni();
    }

    @Override
    public Uni<Void> set(K key, V value, io.quarkus.redis.datasource.string.SetArgs setArgs) {
        // The upcast routes to the value-args overload; without it this call would recurse.
        return set(key, value, (SetArgs) setArgs);
    }

    LettuceCommand<String, Boolean> _set(K key, V value, SetArgs setArgs) {
        nonNull(key, "key");
        nonNull(value, "value");
        nonNull(setArgs, "setArgs");
        io.lettuce.core.SetArgs lettuceArgs = LettuceValueCommandsConverters.toLettuceSetArgs(setArgs);
        return LettuceCommand.of(() -> async.set(marshaller.encode(key), marshaller.encode(value), lettuceArgs),
                AbstractLettuceCommands::isOk);
    }

    @Override
    public Uni<Boolean> setAndChanged(K key, V value) {
        return _set(key, value).toUni();
    }

    @Override
    public Uni<Boolean> setAndChanged(K key, V value, SetArgs setArgs) {
        return _set(key, value, setArgs).toUni();
    }

    @Override
    public Uni<V> setGet(K key, V value) {
        return _setGet(key, value).toUni();
    }

    LettuceCommand<byte[], V> _setGet(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return LettuceCommand.of(() -> async.setGet(marshaller.encode(key), marshaller.encode(value)), this::decodeValue);
    }

    @Override
    public Uni<V> setGet(K key, V value, SetArgs setArgs) {
        return _setGet(key, value, setArgs).toUni();
    }

    @Override
    public Uni<V> setGet(K key, V value, io.quarkus.redis.datasource.string.SetArgs setArgs) {
        // The upcast routes to the value-args overload; without it this call would recurse.
        return setGet(key, value, (SetArgs) setArgs);
    }

    LettuceCommand<byte[], V> _setGet(K key, V value, SetArgs setArgs) {
        nonNull(key, "key");
        nonNull(value, "value");
        nonNull(setArgs, "setArgs");
        io.lettuce.core.SetArgs lettuceArgs = LettuceValueCommandsConverters.toLettuceSetArgs(setArgs);
        return LettuceCommand.of(() -> async.setGet(marshaller.encode(key), marshaller.encode(value), lettuceArgs),
                this::decodeValue);
    }

    @Override
    public Uni<Void> setex(K key, long seconds, V value) {
        return _setex(key, seconds, value).toUni();
    }

    LettuceCommand<String, Void> _setex(K key, long seconds, V value) {
        nonNull(key, "key");
        positive(seconds, "seconds");
        nonNull(value, "value");
        return LettuceCommand.discarding(() -> async.setex(marshaller.encode(key), seconds, marshaller.encode(value)));
    }

    @Override
    public Uni<Boolean> setnx(K key, V value) {
        return _setnx(key, value).toUni();
    }

    LettuceCommand<Boolean, Boolean> _setnx(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return LettuceCommand.of(() -> async.setnx(marshaller.encode(key), marshaller.encode(value)));
    }

    @Override
    public Uni<Long> setrange(K key, long offset, V value) {
        return _setrange(key, offset, value).toUni();
    }

    LettuceCommand<Long, Long> _setrange(K key, long offset, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        positiveOrZero(offset, "offset");
        return LettuceCommand.of(() -> async.setrange(marshaller.encode(key), offset, marshaller.encode(value)));
    }

    @Override
    public Uni<Long> strlen(K key) {
        return _strlen(key).toUni();
    }

    LettuceCommand<Long, Long> _strlen(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.strlen(marshaller.encode(key)));
    }

}
