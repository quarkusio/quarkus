package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.List;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.bitmap.BitFieldArgs;
import io.quarkus.redis.datasource.bitmap.BitMapCommands;
import io.quarkus.redis.datasource.bitmap.ReactiveBitMapCommands;

public class BlockingBitmapCommandsImpl<K> extends AbstractRedisCommandGroup implements BitMapCommands<K> {

    private final ReactiveBitMapCommands<K> reactive;

    public BlockingBitmapCommandsImpl(RedisDataSource ds, ReactiveBitMapCommands<K> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public int setbit(K key, long offset, int value) {
        return reactive.setbit(key, offset, value).await().atMost(timeout);
    }

    @Override
    public long bitcount(K key) {
        return reactive.bitcount(key).await().atMost(timeout);
    }

    @Override
    public long bitcount(K key, long start, long end) {
        return reactive.bitcount(key, start, end).await().atMost(timeout);
    }

    @Override
    public List<Long> bitfield(K key, BitFieldArgs bitFieldArgs) {
        return reactive.bitfield(key, bitFieldArgs).await().atMost(timeout);
    }

    @Override
    public long bitpos(K key, int bit) {
        return reactive.bitpos(key, bit).await().atMost(timeout);
    }

    @Override
    public long bitpos(K key, int bit, long start) {
        return reactive.bitpos(key, bit, start).await().atMost(timeout);
    }

    @Override
    public long bitpos(K key, int bit, long start, long end) {
        return reactive.bitpos(key, bit, start, end).await().atMost(timeout);
    }

    @Override
    public long bitopAnd(K destination, K... keys) {
        return reactive.bitopAnd(destination, keys).await().atMost(timeout);
    }

    @Override
    public long bitopNot(K destination, K source) {
        return reactive.bitopNot(destination, source).await().atMost(timeout);
    }

    @Override
    public long bitopOr(K destination, K... keys) {
        return reactive.bitopOr(destination, keys).await().atMost(timeout);
    }

    @Override
    public long bitopXor(K destination, K... keys) {
        return reactive.bitopXor(destination, keys).await().atMost(timeout);
    }

    @Override
    public int getbit(K key, long offset) {
        return reactive.getbit(key, offset).await().atMost(timeout);
    }

}
