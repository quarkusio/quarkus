package io.quarkus.redis.lettuce.runtime.internal.bitmap;

import static io.quarkus.redis.runtime.datasource.Validation.isBit;
import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.util.List;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.bitmap.BitFieldArgs;
import io.quarkus.redis.datasource.bitmap.ReactiveBitMapCommands;
import io.quarkus.redis.lettuce.runtime.internal.AbstractLettuceCommands;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveBitMapCommandsImpl<K> extends AbstractLettuceCommands<K, K>
        implements ReactiveBitMapCommands<K> {

    private final ReactiveRedisDataSource dataSource;

    public LettuceReactiveBitMapCommandsImpl(ReactiveRedisDataSource dataSource,
            LettuceConnection connection, Type keyType) {
        super(connection, keyType, keyType, new Marshaller(keyType));
        this.dataSource = dataSource;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Long> bitcount(K key) {
        return _bitcount(key).toUni();
    }

    LettuceCommand<Long, Long> _bitcount(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.bitcount(marshaller.encode(key)));
    }

    @Override
    public Uni<Long> bitcount(K key, long start, long end) {
        return _bitcount(key, start, end).toUni();
    }

    LettuceCommand<Long, Long> _bitcount(K key, long start, long end) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.bitcount(marshaller.encode(key), start, end));
    }

    @Override
    public Uni<Integer> getbit(K key, long offset) {
        return _getbit(key, offset).toUni();
    }

    LettuceCommand<Long, Integer> _getbit(K key, long offset) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.getbit(marshaller.encode(key), offset), AbstractLettuceCommands::toInteger);
    }

    @Override
    public Uni<List<Long>> bitfield(K key, BitFieldArgs bitFieldArgs) {
        return _bitfield(key, bitFieldArgs).toUni();
    }

    LettuceCommand<List<Long>, List<Long>> _bitfield(K key, BitFieldArgs bitFieldArgs) {
        nonNull(key, "key");
        nonNull(bitFieldArgs, "bitFieldArgs");
        return LettuceCommand.of(() -> async.bitfield(marshaller.encode(key),
                LettuceBitMapCommandsConverters.toLettuceBitFieldArgs(bitFieldArgs)), AbstractLettuceCommands::decodeList);
    }

    @Override
    public Uni<Long> bitpos(K key, int bit) {
        return _bitpos(key, bit).toUni();
    }

    LettuceCommand<Long, Long> _bitpos(K key, int bit) {
        nonNull(key, "key");
        isBit(bit, "bit");
        return LettuceCommand.of(() -> async.bitpos(marshaller.encode(key), bitToBoolean(bit)));
    }

    @Override
    public Uni<Long> bitpos(K key, int bit, long start) {
        return _bitpos(key, bit, start).toUni();
    }

    LettuceCommand<Long, Long> _bitpos(K key, int bit, long start) {
        nonNull(key, "key");
        isBit(bit, "bit");
        return LettuceCommand.of(() -> async.bitpos(marshaller.encode(key), bitToBoolean(bit), start));
    }

    @Override
    public Uni<Long> bitpos(K key, int bit, long start, long end) {
        return _bitpos(key, bit, start, end).toUni();
    }

    LettuceCommand<Long, Long> _bitpos(K key, int bit, long start, long end) {
        nonNull(key, "key");
        isBit(bit, "bit");
        return LettuceCommand.of(() -> async.bitpos(marshaller.encode(key), bitToBoolean(bit), start, end));
    }

    @SafeVarargs
    @Override
    public final Uni<Long> bitopAnd(K destination, K... keys) {
        return _bitopAnd(destination, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _bitopAnd(K destination, K... keys) {
        nonNull(destination, "destination");
        notNullOrEmpty(keys, "keys");
        return LettuceCommand.of(() -> async.bitopAnd(marshaller.encode(destination), marshaller.encodeAsArray(keys)));
    }

    @Override
    public Uni<Long> bitopNot(K destination, K source) {
        return _bitopNot(destination, source).toUni();
    }

    LettuceCommand<Long, Long> _bitopNot(K destination, K source) {
        nonNull(destination, "destination");
        nonNull(source, "source");
        return LettuceCommand.of(() -> async.bitopNot(marshaller.encode(destination), marshaller.encode(source)));
    }

    @SafeVarargs
    @Override
    public final Uni<Long> bitopOr(K destination, K... keys) {
        return _bitopOr(destination, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _bitopOr(K destination, K... keys) {
        nonNull(destination, "destination");
        notNullOrEmpty(keys, "keys");
        return LettuceCommand.of(() -> async.bitopOr(marshaller.encode(destination), marshaller.encodeAsArray(keys)));
    }

    @SafeVarargs
    @Override
    public final Uni<Long> bitopXor(K destination, K... keys) {
        return _bitopXor(destination, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _bitopXor(K destination, K... keys) {
        nonNull(destination, "destination");
        notNullOrEmpty(keys, "keys");
        return LettuceCommand.of(() -> async.bitopXor(marshaller.encode(destination), marshaller.encodeAsArray(keys)));
    }

    @Override
    public Uni<Integer> setbit(K key, long offset, int value) {
        return _setbit(key, offset, value).toUni();
    }

    LettuceCommand<Long, Integer> _setbit(K key, long offset, int value) {
        nonNull(key, "key");
        isBit(value, "value");
        return LettuceCommand.of(() -> async.setbit(marshaller.encode(key), offset, value), AbstractLettuceCommands::toInteger);
    }

    private boolean bitToBoolean(int value) {
        return value == 1;
    }

}
