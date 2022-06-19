package io.quarkus.redis.datasource.impl;

import java.time.Duration;
import java.util.List;

import io.quarkus.redis.datasource.api.bitmap.BitFieldArgs;
import io.quarkus.redis.datasource.api.bitmap.BitMapCommands;
import io.quarkus.redis.datasource.api.bitmap.ReactiveBitMapCommands;

public class BlockingBitmapCommandsImpl<K> implements BitMapCommands<K> {

    private final ReactiveBitMapCommands<K> reactive;
    private final Duration timeout;

    public BlockingBitmapCommandsImpl(ReactiveBitMapCommands<K> reactive, Duration timeout) {
        this.reactive = reactive;
        this.timeout = timeout;
    }

    @Override
    public int setbit(K key, long offset, int value) {
        return reactive.setbit(key, offset, value).await().atMost(timeout);
    }

    @Override
    public long bitcount(K key) {
        return reactive.bitcount(key)
                .await().atMost(timeout);
    }

    @Override
    public long bitcount(K key, long start, long end) {
        return reactive.bitcount(key, start, end)
                .await().atMost(timeout);
    }

    @Override
    public List<Long> bitfield(K key, BitFieldArgs bitFieldArgs) {
        return reactive.bitfield(key, bitFieldArgs)
                .await().atMost(timeout);
    }

    @Override
    public long bitpos(K key, int bit) {
        return reactive.bitpos(key, bit)
                .await().atMost(timeout);
    }

    @Override
    public long bitpos(K key, int bit, long start) {
        return reactive.bitpos(key, bit, start)
                .await().atMost(timeout);
    }

    @Override
    public long bitpos(K key, int bit, long start, long end) {
        return reactive.bitpos(key, bit, start, end)
                .await().atMost(timeout);
    }

    @Override
    public long bitopAnd(K destination, K... keys) {
        return reactive.bitopAnd(destination, keys)
                .await().atMost(timeout);
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
