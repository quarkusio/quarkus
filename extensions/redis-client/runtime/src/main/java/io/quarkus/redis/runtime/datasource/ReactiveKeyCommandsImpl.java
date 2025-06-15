package io.quarkus.redis.runtime.datasource;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.CopyArgs;
import io.quarkus.redis.datasource.keys.ExpireArgs;
import io.quarkus.redis.datasource.keys.KeyScanArgs;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.keys.ReactiveKeyScanCursor;
import io.quarkus.redis.datasource.keys.RedisValueType;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveKeyCommandsImpl<K> extends AbstractKeyCommands<K> implements ReactiveKeyCommands<K> {

    private final ReactiveRedisDataSource reactive;

    public ReactiveKeyCommandsImpl(ReactiveRedisDataSourceImpl redis, Type k) {
        super(redis, k);
        this.reactive = redis;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
    }

    @Override
    public Uni<Boolean> copy(K source, K destination) {
        return super._copy(source, destination).map(Response::toBoolean);
    }

    @Override
    public Uni<Boolean> copy(K source, K destination, CopyArgs copyArgs) {
        return super._copy(source, destination, copyArgs).map(Response::toBoolean);
    }

    @Override
    public Uni<Integer> del(K... keys) {
        return super._del(keys).map(Response::toInteger);
    }

    @Override
    public Uni<String> dump(K key) {
        return super._dump(key).map(this::decodeStringOrNull);
    }

    @Override
    public Uni<Boolean> exists(K key) {
        return super._exists(key).map(Response::toBoolean);
    }

    @Override
    public Uni<Integer> exists(K... keys) {
        return super._exists(keys).map(Response::toInteger);
    }

    @Override
    public Uni<Boolean> expire(K key, long seconds, ExpireArgs expireArgs) {
        return super._expire(key, seconds, expireArgs).map(Response::toBoolean);
    }

    @Override
    public Uni<Boolean> expire(K key, Duration duration, ExpireArgs expireArgs) {
        return expire(key, duration.toSeconds(), expireArgs);
    }

    @Override
    public Uni<Boolean> expire(K key, long seconds) {
        return expire(key, seconds, new ExpireArgs());
    }

    @Override
    public Uni<Boolean> expire(K key, Duration duration) {
        return expire(key, duration.toSeconds(), new ExpireArgs());
    }

    @Override
    public Uni<Boolean> expireat(K key, long timestamp) {
        return expireat(key, timestamp, new ExpireArgs());
    }

    @Override
    public Uni<Boolean> expireat(K key, Instant timestamp) {
        return expireat(key, timestamp.getEpochSecond(), new ExpireArgs());
    }

    @Override
    public Uni<Boolean> expireat(K key, long timestamp, ExpireArgs expireArgs) {
        return super._expireat(key, timestamp, expireArgs).map(Response::toBoolean);
    }

    @Override
    public Uni<Boolean> expireat(K key, Instant timestamp, ExpireArgs expireArgs) {
        return expireat(key, timestamp.getEpochSecond(), expireArgs);
    }

    @Override
    public Uni<Long> expiretime(K key) {
        return super._expiretime(key).map(r -> decodeExpireResponse(key, r));
    }

    @Override
    public Uni<List<K>> keys(String pattern) {
        return super._keys(pattern).map(this::decodeKeys);
    }

    @Override
    public Uni<Boolean> move(K key, long db) {
        return super._move(key, db).map(Response::toBoolean);
    }

    @Override
    public Uni<Boolean> persist(K key) {
        return super._persist(key).map(Response::toBoolean);
    }

    @Override
    public Uni<Boolean> pexpire(K key, long milliseconds, ExpireArgs expireArgs) {
        return super._pexpire(key, milliseconds, expireArgs).map(Response::toBoolean);
    }

    @Override
    public Uni<Boolean> pexpire(K key, Duration duration, ExpireArgs expireArgs) {
        return pexpire(key, duration.toMillis(), expireArgs);
    }

    @Override
    public Uni<Boolean> pexpire(K key, long ms) {
        return pexpire(key, ms, new ExpireArgs());
    }

    @Override
    public Uni<Boolean> pexpire(K key, Duration duration) {
        return pexpire(key, duration.toMillis(), new ExpireArgs());
    }

    @Override
    public Uni<Boolean> pexpireat(K key, long timestamp) {
        return pexpireat(key, timestamp, new ExpireArgs());
    }

    @Override
    public Uni<Boolean> pexpireat(K key, Instant timestamp) {
        return pexpireat(key, timestamp.toEpochMilli(), new ExpireArgs());
    }

    @Override
    public Uni<Boolean> pexpireat(K key, long timestamp, ExpireArgs expireArgs) {
        return super._pexpireat(key, timestamp, expireArgs).map(Response::toBoolean);
    }

    @Override
    public Uni<Boolean> pexpireat(K key, Instant timestamp, ExpireArgs expireArgs) {
        return pexpireat(key, timestamp.toEpochMilli(), expireArgs);
    }

    @Override
    public Uni<Long> pexpiretime(K key) {
        return super._pexpiretime(key).map(r -> decodeExpireResponse(key, r));
    }

    @Override
    public Uni<Long> pttl(K key) {
        return super._pttl(key).map(r -> decodeExpireResponse(key, r));
    }

    @Override
    public Uni<K> randomkey() {
        return super._randomkey().map(this::decodeK);
    }

    @Override
    public Uni<Void> rename(K key, K newKey) {
        return super._rename(key, newKey).replaceWithVoid();
    }

    @Override
    public Uni<Boolean> renamenx(K key, K newKey) {
        return super._renamenx(key, newKey).map(Response::toBoolean);
    }

    @Override
    public ReactiveKeyScanCursor<K> scan() {
        return new ScanReactiveCursorImpl<>(redis, marshaller, typeOfKey, Collections.emptyList());
    }

    @Override
    public ReactiveKeyScanCursor<K> scan(KeyScanArgs args) {
        nonNull(args, "args");
        return new ScanReactiveCursorImpl<>(redis, marshaller, typeOfKey, args.toArgs());
    }

    @Override
    public Uni<Integer> touch(K... keys) {
        return super._touch(keys).map(Response::toInteger);
    }

    @Override
    public Uni<Long> ttl(K key) {
        return super._ttl(key).map(r -> decodeExpireResponse(key, r));
    }

    @Override
    public Uni<RedisValueType> type(K key) {
        return super._type(key).map(this::decodeRedisType);
    }

    @Override
    public Uni<Integer> unlink(K... keys) {
        return super._unlink(keys).map(Response::toInteger);
    }

}
