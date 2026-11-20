package io.quarkus.redis.lettuce.runtime.internal.geo;

import static io.quarkus.redis.lettuce.runtime.internal.geo.LettuceReactiveGeoCommandsImpl.DEFAULT_INSTANCE;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import io.quarkus.redis.datasource.geo.GeoAddArgs;
import io.quarkus.redis.datasource.geo.GeoItem;
import io.quarkus.redis.datasource.geo.GeoPosition;
import io.quarkus.redis.datasource.geo.GeoRadiusArgs;
import io.quarkus.redis.datasource.geo.GeoRadiusStoreArgs;
import io.quarkus.redis.datasource.geo.GeoSearchArgs;
import io.quarkus.redis.datasource.geo.GeoSearchStoreArgs;
import io.quarkus.redis.datasource.geo.GeoUnit;
import io.quarkus.redis.datasource.geo.ReactiveTransactionalGeoCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceTransactionHolder;
import io.smallrye.mutiny.Uni;

/**
 * Lettuce-backed implementation of {@link ReactiveTransactionalGeoCommands}.
 * <p>
 * A thin transactional shell over {@link LettuceReactiveGeoCommandsImpl}. Each command reuses the
 * non-transactional command-builder seam ({@code reactive._xxx(...)}) for validation and argument
 * conversion, and hands the resulting {@link io.quarkus.redis.lettuce.runtime.internal.LettuceCommand}
 * to the {@link LettuceTransactionHolder}. The command carries the same result mapper the
 * non-transactional path applies, so {@code TransactionResult.get(index)} yields the same Java type
 * as the Vert.x backend.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class LettuceReactiveTransactionalGeoCommandsImpl<K, V>
        implements ReactiveTransactionalGeoCommands<K, V> {

    private final ReactiveTransactionalRedisDataSource dataSource;
    private final LettuceReactiveGeoCommandsImpl<K, V> reactive;
    private final LettuceTransactionHolder tx;

    public LettuceReactiveTransactionalGeoCommandsImpl(ReactiveTransactionalRedisDataSource dataSource,
            LettuceReactiveGeoCommandsImpl<K, V> reactive, LettuceTransactionHolder tx) {
        this.dataSource = dataSource;
        this.reactive = reactive;
        this.tx = tx;
    }

    @Override
    public ReactiveTransactionalRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Void> geoadd(K key, double longitude, double latitude, V member) {
        return geoadd(key, longitude, latitude, member, DEFAULT_INSTANCE);
    }

    @Override
    public Uni<Void> geoadd(K key, GeoPosition position, V member) {
        nonNull(position, "position");
        return geoadd(key, position.longitude, position.latitude, member);
    }

    @Override
    public Uni<Void> geoadd(K key, GeoItem<V> item) {
        nonNull(item, "item");
        return geoadd(key, item.longitude(), item.latitude(), item.member());
    }

    @SafeVarargs
    @Override
    public final Uni<Void> geoadd(K key, GeoItem<V>... items) {
        return tx.enqueue(reactive._geoadd(key, items));
    }

    @Override
    public Uni<Void> geoadd(K key, double longitude, double latitude, V member, GeoAddArgs args) {
        return tx.enqueue(reactive._geoadd(key, longitude, latitude, member, args));
    }

    @Override
    public Uni<Void> geoadd(K key, GeoItem<V> item, GeoAddArgs args) {
        nonNull(item, "item");
        return geoadd(key, item.longitude(), item.latitude(), item.member(), args);
    }

    @SafeVarargs
    @Override
    public final Uni<Void> geoadd(K key, GeoAddArgs args, GeoItem<V>... items) {
        return tx.enqueue(reactive._geoadd(key, args, items));
    }

    @Override
    public Uni<Void> geodist(K key, V from, V to, GeoUnit unit) {
        return tx.enqueue(reactive._geodist(key, from, to, unit));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> geohash(K key, V... members) {
        return tx.enqueue(reactive._geohash(key, members));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> geopos(K key, V... members) {
        return tx.enqueue(reactive._geopos(key, members));
    }

    @Override
    public Uni<Void> georadius(K key, double longitude, double latitude, double radius, GeoUnit unit) {
        return tx.enqueue(reactive._georadius(key, longitude, latitude, radius, unit));
    }

    @Override
    public Uni<Void> georadius(K key, GeoPosition position, double radius, GeoUnit unit) {
        nonNull(position, "position");
        return georadius(key, position.longitude, position.latitude, radius, unit);
    }

    @Override
    public Uni<Void> georadius(K key, double longitude, double latitude, double radius, GeoUnit unit,
            GeoRadiusArgs geoArgs) {
        return tx.enqueue(reactive._georadius(key, longitude, latitude, radius, unit, geoArgs));
    }

    @Override
    public Uni<Void> georadius(K key, GeoPosition position, double radius, GeoUnit unit, GeoRadiusArgs geoArgs) {
        nonNull(position, "position");
        return georadius(key, position.longitude, position.latitude, radius, unit, geoArgs);
    }

    @Override
    public Uni<Void> georadius(K key, double longitude, double latitude, double radius, GeoUnit unit,
            GeoRadiusStoreArgs<K> geoArgs) {
        return tx.enqueue(reactive._georadius(key, longitude, latitude, radius, unit, geoArgs));
    }

    @Override
    public Uni<Void> georadius(K key, GeoPosition position, double radius, GeoUnit unit, GeoRadiusStoreArgs<K> geoArgs) {
        nonNull(position, "position");
        return georadius(key, position.longitude, position.latitude, radius, unit, geoArgs);
    }

    @Override
    public Uni<Void> georadiusbymember(K key, V member, double distance, GeoUnit unit) {
        return tx.enqueue(reactive._georadiusbymember(key, member, distance, unit));
    }

    @Override
    public Uni<Void> georadiusbymember(K key, V member, double distance, GeoUnit unit, GeoRadiusArgs geoArgs) {
        return tx.enqueue(reactive._georadiusbymember(key, member, distance, unit, geoArgs));
    }

    @Override
    public Uni<Void> georadiusbymember(K key, V member, double distance, GeoUnit unit, GeoRadiusStoreArgs<K> geoArgs) {
        return tx.enqueue(reactive._georadiusbymember(key, member, distance, unit, geoArgs));
    }

    @Override
    public Uni<Void> geosearch(K key, GeoSearchArgs<V> args) {
        return tx.enqueue(reactive._geosearch(key, args));
    }

    @Override
    public Uni<Void> geosearchstore(K destination, K key, GeoSearchStoreArgs<V> args, boolean storeDist) {
        return tx.enqueue(reactive._geosearchstore(destination, key, args, storeDist));
    }

}
