package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.List;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.cuckoo.CfInsertArgs;
import io.quarkus.redis.datasource.cuckoo.CfReserveArgs;
import io.quarkus.redis.datasource.cuckoo.CuckooCommands;
import io.quarkus.redis.datasource.cuckoo.ReactiveCuckooCommands;

public class BlockingCuckooCommandsImpl<K, V> extends AbstractRedisCommandGroup implements CuckooCommands<K, V> {

    private final ReactiveCuckooCommands<K, V> reactive;

    public BlockingCuckooCommandsImpl(RedisDataSource ds, ReactiveCuckooCommands<K, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public void cfadd(K key, V value) {
        reactive.cfadd(key, value).await().atMost(timeout);
    }

    @Override
    public boolean cfaddnx(K key, V value) {
        return reactive.cfaddnx(key, value).await().atMost(timeout);
    }

    @Override
    public long cfcount(K key, V value) {
        return reactive.cfcount(key, value).await().atMost(timeout);
    }

    @Override
    public boolean cfdel(K key, V value) {
        return reactive.cfdel(key, value).await().atMost(timeout);
    }

    @Override
    public boolean cfexists(K key, V value) {
        return reactive.cfexists(key, value).await().atMost(timeout);
    }

    @Override
    public void cfinsert(K key, V... values) {
        reactive.cfinsert(key, values).await().atMost(timeout);
    }

    @Override
    public void cfinsert(K key, CfInsertArgs args, V... values) {
        reactive.cfinsert(key, args, values).await().atMost(timeout);
    }

    @Override
    public List<Boolean> cfinsertnx(K key, V... values) {
        return reactive.cfinsertnx(key, values).await().atMost(timeout);
    }

    @Override
    public List<Boolean> cfinsertnx(K key, CfInsertArgs args, V... values) {
        return reactive.cfinsertnx(key, args, values).await().atMost(timeout);
    }

    @Override
    public List<Boolean> cfmexists(K key, V... values) {
        return reactive.cfmexists(key, values).await().atMost(timeout);
    }

    @Override
    public void cfreserve(K key, long capacity) {
        reactive.cfreserve(key, capacity).await().atMost(timeout);
    }

    @Override
    public void cfreserve(K key, long capacity, CfReserveArgs args) {
        reactive.cfreserve(key, capacity, args).await().atMost(timeout);
    }
}
