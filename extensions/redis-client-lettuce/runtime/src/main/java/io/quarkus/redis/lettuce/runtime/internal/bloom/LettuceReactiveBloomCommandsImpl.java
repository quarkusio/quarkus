package io.quarkus.redis.lettuce.runtime.internal.bloom;

import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.util.List;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.bloom.BfInsertArgs;
import io.quarkus.redis.datasource.bloom.BfReserveArgs;
import io.quarkus.redis.datasource.bloom.ReactiveBloomCommands;
import io.quarkus.redis.lettuce.runtime.internal.AbstractLettuceCommands;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveBloomCommandsImpl<K, V> extends AbstractLettuceCommands<K, V>
        implements ReactiveBloomCommands<K, V> {

    private final ReactiveRedisDataSource dataSource;

    public LettuceReactiveBloomCommandsImpl(ReactiveRedisDataSource dataSource,
            LettuceConnection connection, Type keyType, Type valueType) {
        super(connection, keyType, valueType, new Marshaller(keyType, valueType));
        this.dataSource = dataSource;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Boolean> bfadd(K key, V value) {
        return _bfadd(key, value).toUni();
    }

    LettuceCommand<Boolean, Boolean> _bfadd(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return LettuceCommand.of(() -> async.bfAdd(marshaller.encode(key), marshaller.encode(value)));
    }

    @Override
    public Uni<Boolean> bfexists(K key, V value) {
        return _bfexists(key, value).toUni();
    }

    LettuceCommand<Boolean, Boolean> _bfexists(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return LettuceCommand.of(() -> async.bfExists(marshaller.encode(key), marshaller.encode(value)));
    }

    @SafeVarargs
    @Override
    public final Uni<List<Boolean>> bfmadd(K key, V... values) {
        return _bfmadd(key, values).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<Boolean>, List<Boolean>> _bfmadd(K key, V... values) {
        nonNull(key, "key");
        doesNotContainNull(values, "values");
        if (values.length == 0) {
            return LettuceCommand.failing(new IllegalArgumentException("`values` must contain at least one item"));
        }
        return LettuceCommand.of(() -> async.bfMAdd(marshaller.encode(key), marshaller.encodeAsArray(values)),
                AbstractLettuceCommands::nullsToFalse);
    }

    @SafeVarargs
    @Override
    public final Uni<List<Boolean>> bfmexists(K key, V... values) {
        return _bfmexists(key, values).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<Boolean>, List<Boolean>> _bfmexists(K key, V... values) {
        nonNull(key, "key");
        doesNotContainNull(values, "values");
        if (values.length == 0) {
            return LettuceCommand.failing(new IllegalArgumentException("`values` must contain at least one item"));
        }
        return LettuceCommand.of(() -> async.bfMExists(marshaller.encode(key), marshaller.encodeAsArray(values)),
                AbstractLettuceCommands::decodeList);
    }

    @Override
    public Uni<Void> bfreserve(K key, double errorRate, long capacity) {
        return _bfreserve(key, errorRate, capacity, new BfReserveArgs()).toUni();
    }

    @Override
    public Uni<Void> bfreserve(K key, double errorRate, long capacity, BfReserveArgs args) {
        return _bfreserve(key, errorRate, capacity, args).toUni();
    }

    LettuceCommand<String, Void> _bfreserve(K key, double errorRate, long capacity, BfReserveArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");
        io.lettuce.core.probabilistic.arguments.BfReserveArgs lettuceArgs = LettuceBloomCommandsConverters
                .toLettuceBfReserveArgs(args);
        return LettuceCommand.discarding(() -> async.bfReserve(marshaller.encode(key), errorRate, capacity, lettuceArgs));
    }

    @SafeVarargs
    @Override
    public final Uni<List<Boolean>> bfinsert(K key, BfInsertArgs args, V... values) {
        return _bfinsert(key, args, values).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<Boolean>, List<Boolean>> _bfinsert(K key, BfInsertArgs args, V... values) {
        nonNull(key, "key");
        nonNull(args, "args");
        doesNotContainNull(values, "values");
        if (values.length == 0) {
            return LettuceCommand.failing(new IllegalArgumentException("`values` must contain at least one item"));
        }
        io.lettuce.core.probabilistic.arguments.BfInsertArgs lettuceArgs = LettuceBloomCommandsConverters
                .toLettuceBfInsertArgs(args);
        return LettuceCommand.of(() -> async.bfInsert(marshaller.encode(key), lettuceArgs, marshaller.encodeAsArray(values)),
                AbstractLettuceCommands::nullsToFalse);
    }

}
