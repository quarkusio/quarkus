package io.quarkus.redis.lettuce.runtime.internal.cuckoo;

import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.util.List;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.cuckoo.CfInsertArgs;
import io.quarkus.redis.datasource.cuckoo.CfReserveArgs;
import io.quarkus.redis.datasource.cuckoo.ReactiveCuckooCommands;
import io.quarkus.redis.lettuce.runtime.internal.AbstractLettuceCommands;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveCuckooCommandsImpl<K, V> extends AbstractLettuceCommands<K, V>
        implements ReactiveCuckooCommands<K, V> {

    private final ReactiveRedisDataSource dataSource;

    public LettuceReactiveCuckooCommandsImpl(ReactiveRedisDataSource dataSource,
            LettuceConnection connection, Type keyType, Type valueType) {
        super(connection, keyType, valueType, new Marshaller(keyType, valueType));
        this.dataSource = dataSource;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Void> cfadd(K key, V value) {
        return _cfadd(key, value).toUni();
    }

    LettuceCommand<Boolean, Void> _cfadd(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return LettuceCommand.discarding(() -> async.cfAdd(marshaller.encode(key), marshaller.encode(value)));
    }

    @Override
    public Uni<Boolean> cfaddnx(K key, V value) {
        return _cfaddnx(key, value).toUni();
    }

    LettuceCommand<Boolean, Boolean> _cfaddnx(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return LettuceCommand.of(() -> async.cfAddNx(marshaller.encode(key), marshaller.encode(value)));
    }

    @Override
    public Uni<Long> cfcount(K key, V value) {
        return _cfcount(key, value).toUni();
    }

    LettuceCommand<Long, Long> _cfcount(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return LettuceCommand.of(() -> async.cfCount(marshaller.encode(key), marshaller.encode(value)));
    }

    @Override
    public Uni<Boolean> cfdel(K key, V value) {
        return _cfdel(key, value).toUni();
    }

    LettuceCommand<Boolean, Boolean> _cfdel(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return LettuceCommand.of(() -> async.cfDel(marshaller.encode(key), marshaller.encode(value)));
    }

    @Override
    public Uni<Boolean> cfexists(K key, V value) {
        return _cfexists(key, value).toUni();
    }

    LettuceCommand<Boolean, Boolean> _cfexists(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return LettuceCommand.of(() -> async.cfExists(marshaller.encode(key), marshaller.encode(value)));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> cfinsert(K key, V... values) {
        return _cfinsert(key, values).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<Boolean>, Void> _cfinsert(K key, V... values) {
        nonNull(key, "key");
        doesNotContainNull(values, "values");
        if (values.length == 0) {
            return LettuceCommand.failing(new IllegalArgumentException("`values` must contain at least one item"));
        }
        return LettuceCommand.discarding(() -> async.cfInsert(marshaller.encode(key), marshaller.encodeAsArray(values)));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> cfinsert(K key, CfInsertArgs args, V... values) {
        return _cfinsert(key, args, values).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<Boolean>, Void> _cfinsert(K key, CfInsertArgs args, V... values) {
        nonNull(key, "key");
        nonNull(args, "args");
        doesNotContainNull(values, "values");
        if (values.length == 0) {
            return LettuceCommand.failing(new IllegalArgumentException("`values` must contain at least one item"));
        }
        io.lettuce.core.probabilistic.arguments.CfInsertArgs lettuceArgs = LettuceCuckooCommandsConverters
                .toLettuceCfInsertArgs(args);
        return LettuceCommand
                .discarding(() -> async.cfInsert(marshaller.encode(key), lettuceArgs, marshaller.encodeAsArray(values)));
    }

    @SafeVarargs
    @Override
    public final Uni<List<Boolean>> cfinsertnx(K key, V... values) {
        return _cfinsertnx(key, values).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<Boolean>, List<Boolean>> _cfinsertnx(K key, V... values) {
        nonNull(key, "key");
        doesNotContainNull(values, "values");
        if (values.length == 0) {
            return LettuceCommand.failing(new IllegalArgumentException("`values` must contain at least one item"));
        }
        return LettuceCommand.of(() -> async.cfInsertNx(marshaller.encode(key), marshaller.encodeAsArray(values)),
                AbstractLettuceCommands::nullsToFalse);
    }

    @SafeVarargs
    @Override
    public final Uni<List<Boolean>> cfinsertnx(K key, CfInsertArgs args, V... values) {
        return _cfinsertnx(key, args, values).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<Boolean>, List<Boolean>> _cfinsertnx(K key, CfInsertArgs args, V... values) {
        nonNull(key, "key");
        nonNull(args, "args");
        doesNotContainNull(values, "values");
        if (values.length == 0) {
            return LettuceCommand.failing(new IllegalArgumentException("`values` must contain at least one item"));
        }
        io.lettuce.core.probabilistic.arguments.CfInsertArgs lettuceArgs = LettuceCuckooCommandsConverters
                .toLettuceCfInsertArgs(args);
        return LettuceCommand.of(
                () -> async.cfInsertNx(marshaller.encode(key), lettuceArgs, marshaller.encodeAsArray(values)),
                AbstractLettuceCommands::nullsToFalse);
    }

    @SafeVarargs
    @Override
    public final Uni<List<Boolean>> cfmexists(K key, V... values) {
        return _cfmexists(key, values).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<Boolean>, List<Boolean>> _cfmexists(K key, V... values) {
        nonNull(key, "key");
        doesNotContainNull(values, "values");
        if (values.length == 0) {
            return LettuceCommand.failing(new IllegalArgumentException("`values` must contain at least one item"));
        }
        return LettuceCommand.of(() -> async.cfMExists(marshaller.encode(key), marshaller.encodeAsArray(values)),
                AbstractLettuceCommands::decodeList);
    }

    @Override
    public Uni<Void> cfreserve(K key, long capacity) {
        return _cfreserve(key, capacity).toUni();
    }

    LettuceCommand<String, Void> _cfreserve(K key, long capacity) {
        nonNull(key, "key");
        return LettuceCommand.discarding(() -> async.cfReserve(marshaller.encode(key), capacity));
    }

    @Override
    public Uni<Void> cfreserve(K key, long capacity, CfReserveArgs args) {
        return _cfreserve(key, capacity, args).toUni();
    }

    LettuceCommand<String, Void> _cfreserve(K key, long capacity, CfReserveArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");
        io.lettuce.core.probabilistic.arguments.CfReserveArgs lettuceArgs = LettuceCuckooCommandsConverters
                .toLettuceCfReserveArgs(args);
        return LettuceCommand.discarding(() -> async.cfReserve(marshaller.encode(key), capacity, lettuceArgs));
    }

}
