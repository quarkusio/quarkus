package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.bitmap.BitFieldArgs;
import io.quarkus.redis.datasource.bitmap.ReactiveTransactionalBitMapCommands;
import io.quarkus.redis.datasource.bitmap.TransactionalBitMapCommands;

public class BlockingTransactionalBitMapCommandsImpl<K> implements TransactionalBitMapCommands<K> {

    private final ReactiveTransactionalBitMapCommands<K> reactive;

    private final Duration timeout;

    public BlockingTransactionalBitMapCommandsImpl(ReactiveTransactionalBitMapCommands<K> reactive, Duration timeout) {
        this.reactive = reactive;
        this.timeout = timeout;
    }

    @Override
    public void bitcount(K key) {
        this.reactive.bitcount(key).await().atMost(this.timeout);
    }

    @Override
    public void bitcount(K key, long start, long end) {
        this.reactive.bitcount(key, start, end).await().atMost(this.timeout);
    }

    @Override
    public void getbit(K key, long offset) {
        this.reactive.getbit(key, offset).await().atMost(this.timeout);
    }

    @Override
    public void bitfield(K key, BitFieldArgs bitFieldArgs) {
        this.reactive.bitfield(key, bitFieldArgs).await().atMost(this.timeout);
    }

    @Override
    public void bitpos(K key, int valueToLookFor) {
        this.reactive.bitpos(key, valueToLookFor).await().atMost(this.timeout);
    }

    @Override
    public void bitpos(K key, int bit, long start) {
        this.reactive.bitpos(key, bit, start).await().atMost(this.timeout);
    }

    @Override
    public void bitpos(K key, int bit, long start, long end) {
        this.reactive.bitpos(key, bit, start, end).await().atMost(this.timeout);
    }

    @Override
    public void bitopAnd(K destination, K... keys) {
        this.reactive.bitopAnd(destination, keys).await().atMost(this.timeout);
    }

    @Override
    public void bitopNot(K destination, K source) {
        this.reactive.bitopNot(destination, source).await().atMost(this.timeout);
    }

    @Override
    public void bitopOr(K destination, K... keys) {
        this.reactive.bitopOr(destination, keys).await().atMost(this.timeout);
    }

    @Override
    public void bitopXor(K destination, K... keys) {
        this.reactive.bitopXor(destination, keys).await().atMost(this.timeout);
    }

    @Override
    public void setbit(K key, long offset, int value) {
        this.reactive.setbit(key, offset, value).await().atMost(this.timeout);
    }
}
