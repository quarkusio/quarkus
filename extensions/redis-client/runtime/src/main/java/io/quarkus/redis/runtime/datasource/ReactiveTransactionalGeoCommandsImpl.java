package io.quarkus.redis.runtime.datasource;

import static io.quarkus.redis.runtime.datasource.ReactiveGeoCommandsImpl.DEFAULT_INSTANCE;
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
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveTransactionalGeoCommandsImpl<K, V> extends AbstractTransactionalCommands
        implements ReactiveTransactionalGeoCommands<K, V> {

    private final ReactiveGeoCommandsImpl<K, V> reactive;

    public ReactiveTransactionalGeoCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveGeoCommandsImpl<K, V> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
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

    @Override
    public Uni<Void> geoadd(K key, GeoItem<V>... items) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._geoadd(key, items).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> geoadd(K key, double longitude, double latitude, V member, GeoAddArgs args) {
        this.tx.enqueue(r -> r.toLong() == 1L);
        return this.reactive._geoadd(key, longitude, latitude, member, args).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> geoadd(K key, GeoItem<V> item, GeoAddArgs args) {
        nonNull(item, "item");
        return geoadd(key, item.longitude(), item.latitude(), item.member(), args);
    }

    @Override
    public Uni<Void> geoadd(K key, GeoAddArgs args, GeoItem<V>... items) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._geoadd(key, args, items).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> geodist(K key, V from, V to, GeoUnit unit) {
        this.tx.enqueue(this.reactive::decodeDistance);
        return this.reactive._geodist(key, from, to, unit).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> geohash(K key, V... members) {
        this.tx.enqueue(this.reactive::decodeHashList);
        return this.reactive._geohash(key, members).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> geopos(K key, V... members) {
        this.tx.enqueue(this.reactive::decodeGeoPositions);
        return this.reactive._geopos(key, members).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Deprecated
    @Override
    public Uni<Void> georadius(K key, double longitude, double latitude, double radius, GeoUnit unit) {
        this.tx.enqueue(this.reactive::decodeRadiusSet);
        return this.reactive._georadius(key, longitude, latitude, radius, unit).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Deprecated
    @Override
    public Uni<Void> georadius(K key, GeoPosition position, double radius, GeoUnit unit) {
        nonNull(position, "position");
        return georadius(key, position.longitude, position.latitude, radius, unit);
    }

    @Deprecated
    @Override
    public Uni<Void> georadius(K key, double longitude, double latitude, double radius, GeoUnit unit,
            GeoRadiusArgs geoArgs) {
        this.tx.enqueue(r -> reactive.decodeAsListOfGeoValues(r, geoArgs.hasDistance(), geoArgs.hasCoordinates(),
                geoArgs.hasHash()));
        return this.reactive._georadius(key, longitude, latitude, radius, unit, geoArgs).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Deprecated
    @Override
    public Uni<Void> georadius(K key, GeoPosition position, double radius, GeoUnit unit, GeoRadiusArgs geoArgs) {
        nonNull(position, "position");
        return georadius(key, position.longitude, position.latitude, radius, unit, geoArgs);
    }

    @Deprecated
    @Override
    public Uni<Void> georadius(K key, double longitude, double latitude, double radius, GeoUnit unit,
            GeoRadiusStoreArgs<K> geoArgs) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._georadius(key, longitude, latitude, radius, unit, geoArgs).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Deprecated
    @Override
    public Uni<Void> georadius(K key, GeoPosition position, double radius, GeoUnit unit,
            GeoRadiusStoreArgs<K> geoArgs) {
        nonNull(position, "position");
        return georadius(key, position.longitude, position.latitude, radius, unit, geoArgs);
    }

    @Deprecated
    @Override
    public Uni<Void> georadiusbymember(K key, V member, double distance, GeoUnit unit) {
        this.tx.enqueue(this.reactive::decodeRadiusSet);
        return this.reactive._georadiusbymember(key, member, distance, unit).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Deprecated
    @Override
    public Uni<Void> georadiusbymember(K key, V member, double distance, GeoUnit unit, GeoRadiusArgs geoArgs) {
        this.tx.enqueue(r -> reactive.decodeAsListOfGeoValues(r, geoArgs.hasDistance(), geoArgs.hasCoordinates(),
                geoArgs.hasHash()));
        return this.reactive._georadiusbymember(key, member, distance, unit, geoArgs).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Deprecated
    @Override
    public Uni<Void> georadiusbymember(K key, V member, double distance, GeoUnit unit, GeoRadiusStoreArgs<K> geoArgs) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._georadiusbymember(key, member, distance, unit, geoArgs).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> geosearch(K key, GeoSearchArgs<V> args) {
        this.tx.enqueue(
                r -> reactive.decodeAsListOfGeoValues(r, args.hasDistance(), args.hasCoordinates(), args.hasHash()));
        return this.reactive._geosearch(key, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> geosearchstore(K destination, K key, GeoSearchStoreArgs<V> args, boolean storeDist) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._geosearchstore(destination, key, args, storeDist).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }
}
