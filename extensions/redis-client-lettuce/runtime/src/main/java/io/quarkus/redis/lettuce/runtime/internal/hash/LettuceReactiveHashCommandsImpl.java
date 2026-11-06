package io.quarkus.redis.lettuce.runtime.internal.hash;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.quarkus.redis.runtime.datasource.Validation.positive;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.lettuce.core.KeyValue;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.ScanArgs;
import io.quarkus.redis.datasource.hash.ReactiveHashCommands;
import io.quarkus.redis.datasource.hash.ReactiveHashScanCursor;
import io.quarkus.redis.lettuce.runtime.internal.AbstractLettuceCommands;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommonConverters;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveHashCommandsImpl<K, F, V> extends AbstractLettuceCommands<K, V>
        implements ReactiveHashCommands<K, F, V> {

    private final ReactiveRedisDataSource dataSource;
    private final Type fieldType;

    public LettuceReactiveHashCommandsImpl(ReactiveRedisDataSource dataSource,
            LettuceConnection connection, Type keyType, Type fieldType, Type valueType) {
        super(connection, keyType, valueType, new Marshaller(keyType, fieldType, valueType));
        this.dataSource = dataSource;
        this.fieldType = fieldType;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return dataSource;
    }

    @SafeVarargs
    @Override
    public final Uni<Integer> hdel(K key, F... fields) {
        return _hdel(key, fields).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Integer> _hdel(K key, F... fields) {
        nonNull(key, "key");
        notNullOrEmpty(fields, "fields");
        doesNotContainNull(fields, "fields");
        return LettuceCommand.of(() -> async.hdel(marshaller.encode(key), marshaller.encodeAsArray(fields)),
                AbstractLettuceCommands::toInteger);
    }

    @Override
    public Uni<Boolean> hexists(K key, F field) {
        return _hexists(key, field).toUni();
    }

    LettuceCommand<Boolean, Boolean> _hexists(K key, F field) {
        nonNull(key, "key");
        nonNull(field, "field");
        return LettuceCommand.of(() -> async.hexists(marshaller.encode(key), marshaller.encode(field)));
    }

    @Override
    public Uni<V> hget(K key, F field) {
        return _hget(key, field).toUni();
    }

    LettuceCommand<byte[], V> _hget(K key, F field) {
        nonNull(key, "key");
        nonNull(field, "field");
        return LettuceCommand.of(() -> async.hget(marshaller.encode(key), marshaller.encode(field)), this::decodeValue);
    }

    @Override
    public Uni<Long> hincrby(K key, F field, long amount) {
        return _hincrby(key, field, amount).toUni();
    }

    LettuceCommand<Long, Long> _hincrby(K key, F field, long amount) {
        nonNull(key, "key");
        nonNull(field, "field");
        return LettuceCommand.of(() -> async.hincrby(marshaller.encode(key), marshaller.encode(field), amount));
    }

    @Override
    public Uni<Double> hincrbyfloat(K key, F field, double amount) {
        return _hincrbyfloat(key, field, amount).toUni();
    }

    LettuceCommand<Double, Double> _hincrbyfloat(K key, F field, double amount) {
        nonNull(key, "key");
        nonNull(field, "field");
        return LettuceCommand.of(() -> async.hincrbyfloat(marshaller.encode(key), marshaller.encode(field), amount));
    }

    @Override
    public Uni<Map<F, V>> hgetall(K key) {
        return _hgetall(key).toUni();
    }

    LettuceCommand<Map<byte[], byte[]>, Map<F, V>> _hgetall(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.hgetall(marshaller.encode(key)), map -> decodeMap(fieldType, map));
    }

    @Override
    public Uni<List<F>> hkeys(K key) {
        return _hkeys(key).toUni();
    }

    LettuceCommand<List<byte[]>, List<F>> _hkeys(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.hkeys(marshaller.encode(key)), this::decodeListOfField);
    }

    @Override
    public Uni<Long> hlen(K key) {
        return _hlen(key).toUni();
    }

    LettuceCommand<Long, Long> _hlen(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.hlen(marshaller.encode(key)));
    }

    @SafeVarargs
    @Override
    public final Uni<Map<F, V>> hmget(K key, F... fields) {
        return _hmget(key, fields).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<KeyValue<byte[], byte[]>>, Map<F, V>> _hmget(K key, F... fields) {
        nonNull(key, "key");
        doesNotContainNull(fields, "fields");
        if (fields.length == 0) {
            return LettuceCommand.failing(new IllegalArgumentException("`fields` must not be empty"));
        }
        byte[][] encodedFields = marshaller.encodeAsArray(fields);
        return LettuceCommand.of(() -> async.hmget(marshaller.encode(key), encodedFields),
                r -> decodeOrderedMap(fields, r));
    }

    @Deprecated
    @Override
    public Uni<Void> hmset(K key, Map<F, V> map) {
        return _hmset(key, map).toUni();
    }

    LettuceCommand<String, Void> _hmset(K key, Map<F, V> map) {
        nonNull(key, "key");
        nonNull(map, "map");
        if (map.isEmpty()) {
            return LettuceCommand.failing(new IllegalArgumentException("`map` must not be empty"));
        }
        return LettuceCommand.discarding(() -> async.hmset(marshaller.encode(key), encodeMapWithNullableValues(map)));
    }

    @Override
    public Uni<F> hrandfield(K key) {
        return _hrandfield(key).toUni();
    }

    LettuceCommand<byte[], F> _hrandfield(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.hrandfield(marshaller.encode(key)), this::decodeF);
    }

    @Override
    public Uni<List<F>> hrandfield(K key, long count) {
        return _hrandfield(key, count).toUni();
    }

    LettuceCommand<List<byte[]>, List<F>> _hrandfield(K key, long count) {
        nonNull(key, "key");
        positive(count, "count");
        return LettuceCommand.of(() -> async.hrandfield(marshaller.encode(key), count), this::decodeListOfField);
    }

    @Override
    public Uni<Map<F, V>> hrandfieldWithValues(K key, long count) {
        return _hrandfieldWithValues(key, count).toUni();
    }

    LettuceCommand<List<KeyValue<byte[], byte[]>>, Map<F, V>> _hrandfieldWithValues(K key, long count) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.hrandfieldWithvalues(marshaller.encode(key), count),
                this::decodeFieldWithValueMap);
    }

    @Override
    public ReactiveHashScanCursor<F, V> hscan(K key) {
        nonNull(key, "key");
        return new LettuceReactiveHashScanCursorImpl<>(async, marshaller.encode(key), map -> decodeMap(fieldType, map));
    }

    @Override
    public ReactiveHashScanCursor<F, V> hscan(K key, ScanArgs scanArgs) {
        nonNull(key, "key");
        nonNull(scanArgs, "scanArgs");
        return new LettuceReactiveHashScanCursorImpl<>(async, marshaller.encode(key),
                LettuceCommonConverters.toLettuceScanArgs(scanArgs), map -> decodeMap(fieldType, map));
    }

    @Override
    public Uni<Boolean> hset(K key, F field, V value) {
        return _hset(key, field, value).toUni();
    }

    LettuceCommand<Boolean, Boolean> _hset(K key, F field, V value) {
        nonNull(key, "key");
        nonNull(field, "field");
        nonNull(value, "value");
        return LettuceCommand.of(
                () -> async.hset(marshaller.encode(key), marshaller.encode(field), marshaller.encode(value)));
    }

    @Override
    public Uni<Long> hset(K key, Map<F, V> map) {
        return _hset(key, map).toUni();
    }

    LettuceCommand<Long, Long> _hset(K key, Map<F, V> map) {
        nonNull(key, "key");
        nonNull(map, "map");
        if (map.isEmpty()) {
            return LettuceCommand.failing(new IllegalArgumentException("`map` must not be empty"));
        }
        return LettuceCommand.of(() -> async.hset(marshaller.encode(key), encodeMap(map)));
    }

    @Override
    public Uni<Boolean> hsetnx(K key, F field, V value) {
        return _hsetnx(key, field, value).toUni();
    }

    LettuceCommand<Boolean, Boolean> _hsetnx(K key, F field, V value) {
        nonNull(key, "key");
        nonNull(field, "field");
        nonNull(value, "value");
        return LettuceCommand.of(
                () -> async.hsetnx(marshaller.encode(key), marshaller.encode(field), marshaller.encode(value)));
    }

    @Override
    public Uni<Long> hstrlen(K key, F field) {
        return _hstrlen(key, field).toUni();
    }

    LettuceCommand<Long, Long> _hstrlen(K key, F field) {
        nonNull(key, "key");
        nonNull(field, "field");
        return LettuceCommand.of(() -> async.hstrlen(marshaller.encode(key), marshaller.encode(field)));
    }

    @Override
    public Uni<List<V>> hvals(K key) {
        return _hvals(key).toUni();
    }

    LettuceCommand<List<byte[]>, List<V>> _hvals(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.hvals(marshaller.encode(key)), this::decodeListOfValues);
    }

    private F decodeF(byte[] bytes) {
        return marshaller.decode(fieldType, bytes);
    }

    private Map<F, V> decodeFieldWithValueMap(List<KeyValue<byte[], byte[]>> entries) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<F, V> map = LinkedHashMap.newLinkedHashMap(entries.size());
        for (KeyValue<byte[], byte[]> entry : entries) {
            map.put(decodeF(entry.getKey()), decodeValue(entry.getValueOrElse(null)));
        }
        return Collections.unmodifiableMap(map);
    }

    private List<F> decodeListOfField(List<byte[]> list) {
        return list.stream().map(this::decodeF).toList();
    }

}
