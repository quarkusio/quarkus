package io.quarkus.redis.lettuce.runtime.internal.list;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.quarkus.redis.runtime.datasource.Validation.positiveOrZero;
import static io.quarkus.redis.runtime.datasource.Validation.validateTimeout;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.SortArgs;
import io.quarkus.redis.datasource.list.KeyValue;
import io.quarkus.redis.datasource.list.LPosArgs;
import io.quarkus.redis.datasource.list.Position;
import io.quarkus.redis.datasource.list.ReactiveListCommands;
import io.quarkus.redis.lettuce.runtime.internal.AbstractLettuceCommands;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommonConverters;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionPool;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveListCommandsImpl<K, V> extends AbstractLettuceCommands<K, V>
        implements ReactiveListCommands<K, V> {

    private static final SortArgs DEFAULT_SORT_ARGS = new SortArgs();

    private final ReactiveRedisDataSource dataSource;

    public LettuceReactiveListCommandsImpl(ReactiveRedisDataSource dataSource,
            LettuceConnection connection, LettuceConnectionPool pool, Type keyType,
            Type valueType) {
        super(connection, keyType, valueType, new Marshaller(keyType, valueType), pool);
        this.dataSource = dataSource;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<V> blmove(K source, K destination, Position positionInSource, Position positionInDest, Duration timeout) {
        return blocking(cmds -> _blmove(cmds, source, destination, positionInSource, positionInDest, timeout));
    }

    LettuceCommand<byte[], V> _blmove(K source, K destination, Position positionInSource, Position positionInDest,
            Duration timeout) {
        return _blmove(async, source, destination, positionInSource, positionInDest, timeout);
    }

    LettuceCommand<byte[], V> _blmove(RedisClusterAsyncCommands<byte[], byte[]> cmds, K source, K destination,
            Position positionInSource, Position positionInDest, Duration timeout) {
        nonNull(source, "source");
        nonNull(destination, "destination");
        nonNull(positionInSource, "positionInSource");
        nonNull(positionInDest, "positionInDest");
        validateTimeout(timeout, "timeout");
        io.lettuce.core.LMoveArgs args = LettuceListCommandsConverters.toLettuceLMoveArgs(positionInSource, positionInDest);
        return LettuceCommand.of(() -> {
            if (isWholeSeconds(timeout)) {
                return cmds.blmove(marshaller.encode(source), marshaller.encode(destination), args, timeout.getSeconds());
            }
            return cmds.blmove(marshaller.encode(source), marshaller.encode(destination), args, toFractionalSeconds(timeout));
        }, this::decodeValue);
    }

    @SafeVarargs
    @Override
    public final Uni<KeyValue<K, V>> blmpop(Duration timeout, Position position, K... keys) {
        return blocking(cmds -> _blmpop(cmds, timeout, position, keys));
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], List<byte[]>>, KeyValue<K, V>> _blmpop(Duration timeout,
            Position position, K... keys) {
        return _blmpop(async, timeout, position, keys);
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], List<byte[]>>, KeyValue<K, V>> _blmpop(
            RedisClusterAsyncCommands<byte[], byte[]> cmds, Duration timeout, Position position, K... keys) {
        nonNull(position, "position");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validateTimeout(timeout, "timeout");
        io.lettuce.core.LMPopArgs args = LettuceListCommandsConverters.toLettuceLMPopArgs(position);
        return LettuceCommand.of(() -> {
            if (isWholeSeconds(timeout)) {
                return cmds.blmpop(timeout.getSeconds(), args, marshaller.encodeAsArray(keys));
            }
            return cmds.blmpop(toFractionalSeconds(timeout), args, marshaller.encodeAsArray(keys));
        }, this::toFirstKeyValue);
    }

    @SafeVarargs
    @Override
    public final Uni<List<KeyValue<K, V>>> blmpop(Duration timeout, Position position, int count, K... keys) {
        return blocking(cmds -> _blmpop(cmds, timeout, position, count, keys));
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], List<byte[]>>, List<KeyValue<K, V>>> _blmpop(Duration timeout,
            Position position, int count, K... keys) {
        return _blmpop(async, timeout, position, count, keys);
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], List<byte[]>>, List<KeyValue<K, V>>> _blmpop(
            RedisClusterAsyncCommands<byte[], byte[]> cmds, Duration timeout, Position position, int count, K... keys) {
        nonNull(position, "position");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validateTimeout(timeout, "timeout");
        positive(count, "count");
        io.lettuce.core.LMPopArgs args = LettuceListCommandsConverters.toLettuceLMPopArgs(position, count);
        return LettuceCommand.of(() -> {
            if (isWholeSeconds(timeout)) {
                return cmds.blmpop(timeout.getSeconds(), args, marshaller.encodeAsArray(keys));
            }
            return cmds.blmpop(toFractionalSeconds(timeout), args, marshaller.encodeAsArray(keys));
        }, this::toKeyValueList);
    }

    @SafeVarargs
    @Override
    public final Uni<KeyValue<K, V>> blpop(Duration timeout, K... keys) {
        return blocking(cmds -> _blpop(cmds, timeout, keys));
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], byte[]>, KeyValue<K, V>> _blpop(Duration timeout, K... keys) {
        return _blpop(async, timeout, keys);
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], byte[]>, KeyValue<K, V>> _blpop(
            RedisClusterAsyncCommands<byte[], byte[]> cmds, Duration timeout, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validateTimeout(timeout, "timeout");
        return LettuceCommand.of(() -> {
            if (isWholeSeconds(timeout)) {
                return cmds.blpop(timeout.getSeconds(), marshaller.encodeAsArray(keys));
            }
            return cmds.blpop(toFractionalSeconds(timeout), marshaller.encodeAsArray(keys));
        }, this::toKeyValue);
    }

    @SafeVarargs
    @Override
    public final Uni<KeyValue<K, V>> brpop(Duration timeout, K... keys) {
        return blocking(cmds -> _brpop(cmds, timeout, keys));
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], byte[]>, KeyValue<K, V>> _brpop(Duration timeout, K... keys) {
        return _brpop(async, timeout, keys);
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], byte[]>, KeyValue<K, V>> _brpop(
            RedisClusterAsyncCommands<byte[], byte[]> cmds, Duration timeout, K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        validateTimeout(timeout, "timeout");
        return LettuceCommand.of(() -> {
            if (isWholeSeconds(timeout)) {
                return cmds.brpop(timeout.getSeconds(), marshaller.encodeAsArray(keys));
            }
            return cmds.brpop(toFractionalSeconds(timeout), marshaller.encodeAsArray(keys));
        }, this::toKeyValue);
    }

    @Deprecated
    @Override
    public Uni<V> brpoplpush(Duration timeout, K source, K destination) {
        return blocking(cmds -> _brpoplpush(cmds, timeout, source, destination));
    }

    LettuceCommand<byte[], V> _brpoplpush(Duration timeout, K source, K destination) {
        return _brpoplpush(async, timeout, source, destination);
    }

    LettuceCommand<byte[], V> _brpoplpush(RedisClusterAsyncCommands<byte[], byte[]> cmds, Duration timeout, K source,
            K destination) {
        validateTimeout(timeout, "timeout");
        nonNull(source, "source");
        nonNull(destination, "destination");
        return LettuceCommand.of(() -> {
            if (isWholeSeconds(timeout)) {
                return cmds.brpoplpush(timeout.getSeconds(), marshaller.encode(source), marshaller.encode(destination));
            }
            return cmds.brpoplpush(toFractionalSeconds(timeout), marshaller.encode(source), marshaller.encode(destination));
        }, this::decodeValue);
    }

    @Override
    public Uni<V> lindex(K key, long index) {
        return _lindex(key, index).toUni();
    }

    LettuceCommand<byte[], V> _lindex(K key, long index) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.lindex(marshaller.encode(key), index), this::decodeValue);
    }

    @Override
    public Uni<Long> linsertBeforePivot(K key, V pivot, V element) {
        return _linsertBeforePivot(key, pivot, element).toUni();
    }

    LettuceCommand<Long, Long> _linsertBeforePivot(K key, V pivot, V element) {
        nonNull(key, "key");
        nonNull(pivot, "pivot");
        nonNull(element, "element");
        return LettuceCommand.of(
                () -> async.linsert(marshaller.encode(key), true, marshaller.encode(pivot), marshaller.encode(element)));
    }

    @Override
    public Uni<Long> linsertAfterPivot(K key, V pivot, V element) {
        return _linsertAfterPivot(key, pivot, element).toUni();
    }

    LettuceCommand<Long, Long> _linsertAfterPivot(K key, V pivot, V element) {
        nonNull(key, "key");
        nonNull(pivot, "pivot");
        nonNull(element, "element");
        return LettuceCommand.of(
                () -> async.linsert(marshaller.encode(key), false, marshaller.encode(pivot), marshaller.encode(element)));
    }

    @Override
    public Uni<Long> llen(K key) {
        return _llen(key).toUni();
    }

    LettuceCommand<Long, Long> _llen(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.llen(marshaller.encode(key)));
    }

    @Override
    public Uni<V> lmove(K source, K destination, Position positionInSource, Position positionInDestination) {
        return _lmove(source, destination, positionInSource, positionInDestination).toUni();
    }

    LettuceCommand<byte[], V> _lmove(K source, K destination, Position positionInSource, Position positionInDest) {
        nonNull(source, "source");
        nonNull(destination, "destination");
        nonNull(positionInSource, "positionInSource");
        nonNull(positionInDest, "positionInDest");
        io.lettuce.core.LMoveArgs args = LettuceListCommandsConverters.toLettuceLMoveArgs(positionInSource, positionInDest);
        return LettuceCommand.of(() -> async.lmove(marshaller.encode(source), marshaller.encode(destination), args),
                this::decodeValue);
    }

    @SafeVarargs
    @Override
    public final Uni<KeyValue<K, V>> lmpop(Position position, K... keys) {
        return _lmpop(position, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], List<byte[]>>, KeyValue<K, V>> _lmpop(Position position,
            K... keys) {
        nonNull(position, "position");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        io.lettuce.core.LMPopArgs args = LettuceListCommandsConverters.toLettuceLMPopArgs(position);
        return LettuceCommand.of(() -> async.lmpop(args, marshaller.encodeAsArray(keys)), this::toFirstKeyValue);
    }

    @SafeVarargs
    @Override
    public final Uni<List<KeyValue<K, V>>> lmpop(Position position, int count, K... keys) {
        return _lmpop(position, count, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<io.lettuce.core.KeyValue<byte[], List<byte[]>>, List<KeyValue<K, V>>> _lmpop(Position position,
            int count, K... keys) {
        nonNull(position, "position");
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        positive(count, "count");
        io.lettuce.core.LMPopArgs args = LettuceListCommandsConverters.toLettuceLMPopArgs(position, count);
        return LettuceCommand.of(() -> async.lmpop(args, marshaller.encodeAsArray(keys)), this::toKeyValueList);
    }

    @Override
    public Uni<V> lpop(K key) {
        return _lpop(key).toUni();
    }

    LettuceCommand<byte[], V> _lpop(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.lpop(marshaller.encode(key)), this::decodeValue);
    }

    @Override
    public Uni<List<V>> lpop(K key, int count) {
        return _lpop(key, count).toUni();
    }

    LettuceCommand<List<byte[]>, List<V>> _lpop(K key, int count) {
        nonNull(key, "key");
        positive(count, "count");
        return LettuceCommand.of(() -> async.lpop(marshaller.encode(key), count), this::decodeListOfValues);
    }

    @Override
    public Uni<Long> lpos(K key, V element) {
        return _lpos(key, element).toUni();
    }

    LettuceCommand<Long, Long> _lpos(K key, V element) {
        nonNull(key, "key");
        nonNull(element, "element");
        return LettuceCommand.of(() -> async.lpos(marshaller.encode(key), marshaller.encode(element)));
    }

    @Override
    public Uni<Long> lpos(K key, V element, LPosArgs args) {
        return _lpos(key, element, args).toUni();
    }

    LettuceCommand<Long, Long> _lpos(K key, V element, LPosArgs args) {
        nonNull(key, "key");
        nonNull(element, "element");
        nonNull(args, "args");
        io.lettuce.core.LPosArgs lettuceArgs = LettuceListCommandsConverters.toLettuceLPosArgs(args);
        return LettuceCommand.of(() -> async.lpos(marshaller.encode(key), marshaller.encode(element), lettuceArgs));
    }

    @Override
    public Uni<List<Long>> lpos(K key, V element, int count) {
        return _lpos(key, element, count).toUni();
    }

    LettuceCommand<List<Long>, List<Long>> _lpos(K key, V element, int count) {
        nonNull(key, "key");
        nonNull(element, "element");
        positiveOrZero(count, "count"); // 0 -> All matches
        return LettuceCommand.of(() -> async.lpos(marshaller.encode(key), marshaller.encode(element), count),
                AbstractLettuceCommands::decodeList);
    }

    @Override
    public Uni<List<Long>> lpos(K key, V element, int count, LPosArgs args) {
        return _lpos(key, element, count, args).toUni();
    }

    LettuceCommand<List<Long>, List<Long>> _lpos(K key, V element, int count, LPosArgs args) {
        nonNull(key, "key");
        nonNull(element, "element");
        nonNull(args, "args");
        positiveOrZero(count, "count"); // 0 -> All matches
        io.lettuce.core.LPosArgs lettuceArgs = LettuceListCommandsConverters.toLettuceLPosArgs(args);
        return LettuceCommand.of(() -> async.lpos(marshaller.encode(key), marshaller.encode(element), count, lettuceArgs),
                AbstractLettuceCommands::decodeList);
    }

    @SafeVarargs
    @Override
    public final Uni<Long> lpush(K key, V... elements) {
        return _lpush(key, elements).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _lpush(K key, V... elements) {
        nonNull(key, "key");
        notNullOrEmpty(elements, "elements");
        doesNotContainNull(elements, "elements");
        return LettuceCommand.of(() -> async.lpush(marshaller.encode(key), marshaller.encodeAsArray(elements)));
    }

    @SafeVarargs
    @Override
    public final Uni<Long> lpushx(K key, V... elements) {
        return _lpushx(key, elements).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _lpushx(K key, V... elements) {
        nonNull(key, "key");
        notNullOrEmpty(elements, "elements");
        doesNotContainNull(elements, "elements");
        return LettuceCommand.of(() -> async.lpushx(marshaller.encode(key), marshaller.encodeAsArray(elements)));
    }

    @Override
    public Uni<List<V>> lrange(K key, long start, long stop) {
        return _lrange(key, start, stop).toUni();
    }

    LettuceCommand<List<byte[]>, List<V>> _lrange(K key, long start, long stop) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.lrange(marshaller.encode(key), start, stop), this::decodeListOfValues);
    }

    @Override
    public Uni<Long> lrem(K key, long count, V element) {
        return _lrem(key, count, element).toUni();
    }

    LettuceCommand<Long, Long> _lrem(K key, long count, V element) {
        nonNull(key, "key");
        nonNull(element, "element");
        return LettuceCommand.of(() -> async.lrem(marshaller.encode(key), count, marshaller.encode(element)));
    }

    @Override
    public Uni<Void> lset(K key, long index, V element) {
        return _lset(key, index, element).toUni();
    }

    LettuceCommand<String, Void> _lset(K key, long index, V element) {
        nonNull(key, "key");
        nonNull(element, "element");
        return LettuceCommand.discarding(() -> async.lset(marshaller.encode(key), index, marshaller.encode(element)));
    }

    @Override
    public Uni<Void> ltrim(K key, long start, long stop) {
        return _ltrim(key, start, stop).toUni();
    }

    LettuceCommand<String, Void> _ltrim(K key, long start, long stop) {
        nonNull(key, "key");
        return LettuceCommand.discarding(() -> async.ltrim(marshaller.encode(key), start, stop));
    }

    @Override
    public Uni<V> rpop(K key) {
        return _rpop(key).toUni();
    }

    LettuceCommand<byte[], V> _rpop(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.rpop(marshaller.encode(key)), this::decodeValue);
    }

    @Override
    public Uni<List<V>> rpop(K key, int count) {
        return _rpop(key, count).toUni();
    }

    LettuceCommand<List<byte[]>, List<V>> _rpop(K key, int count) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.rpop(marshaller.encode(key), count), this::decodeListOfValues);
    }

    @Deprecated
    @Override
    public Uni<V> rpoplpush(K source, K destination) {
        return _rpoplpush(source, destination).toUni();
    }

    LettuceCommand<byte[], V> _rpoplpush(K source, K destination) {
        nonNull(source, "source");
        nonNull(destination, "destination");
        return LettuceCommand.of(() -> async.rpoplpush(marshaller.encode(source), marshaller.encode(destination)),
                this::decodeValue);
    }

    @SafeVarargs
    @Override
    public final Uni<Long> rpush(K key, V... values) {
        return _rpush(key, values).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _rpush(K key, V... values) {
        nonNull(key, "key");
        notNullOrEmpty(values, "values");
        doesNotContainNull(values, "values");
        return LettuceCommand.of(() -> async.rpush(marshaller.encode(key), marshaller.encodeAsArray(values)));
    }

    @SafeVarargs
    @Override
    public final Uni<Long> rpushx(K key, V... values) {
        return _rpushx(key, values).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _rpushx(K key, V... values) {
        nonNull(key, "key");
        notNullOrEmpty(values, "values");
        doesNotContainNull(values, "values");
        return LettuceCommand.of(() -> async.rpushx(marshaller.encode(key), marshaller.encodeAsArray(values)));
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

    private KeyValue<K, V> toKeyValue(io.lettuce.core.KeyValue<byte[], byte[]> kv) {
        if (kv == null || kv.isEmpty()) {
            return null;
        }
        return KeyValue.of(decodeKey(kv.getKey()), decodeValue(kv.getValueOrElse(null)));
    }

    private KeyValue<K, V> toFirstKeyValue(io.lettuce.core.KeyValue<byte[], List<byte[]>> kv) {
        if (kv == null || kv.isEmpty()) {
            return null;
        }
        List<byte[]> values = kv.getValue();
        if (values == null || values.isEmpty()) {
            return null;
        }
        return KeyValue.of(decodeKey(kv.getKey()), decodeValue(values.getFirst()));
    }

    private List<KeyValue<K, V>> toKeyValueList(io.lettuce.core.KeyValue<byte[], List<byte[]>> kv) {
        if (kv == null || kv.isEmpty()) {
            return Collections.emptyList();
        }
        K key = decodeKey(kv.getKey());
        return decodeList(kv.getValue(), value -> KeyValue.of(key, decodeValue(value)));
    }

}
