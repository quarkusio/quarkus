package io.quarkus.redis.runtime.datasource;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.geo.GeoAddArgs;
import io.quarkus.redis.datasource.geo.GeoItem;
import io.quarkus.redis.datasource.geo.GeoPosition;
import io.quarkus.redis.datasource.geo.GeoRadiusArgs;
import io.quarkus.redis.datasource.geo.GeoRadiusStoreArgs;
import io.quarkus.redis.datasource.geo.GeoSearchArgs;
import io.quarkus.redis.datasource.geo.GeoSearchStoreArgs;
import io.quarkus.redis.datasource.geo.GeoUnit;
import io.quarkus.redis.datasource.geo.GeoValue;
import io.quarkus.redis.datasource.geo.ReactiveGeoCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveGeoCommandsImpl<K, V> extends AbstractGeoCommands<K, V> implements ReactiveGeoCommands<K, V> {

    static final GeoAddArgs DEFAULT_INSTANCE = new GeoAddArgs();
    private final ReactiveRedisDataSource reactive;

    public ReactiveGeoCommandsImpl(ReactiveRedisDataSourceImpl redis, Type k, Type v) {
        super(redis, k, v);
        this.reactive = redis;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
    }

    @Override
    public Uni<Boolean> geoadd(K key, double longitude, double latitude, V member) {
        return geoadd(key, longitude, latitude, member, DEFAULT_INSTANCE);
    }

    @Override
    public Uni<Boolean> geoadd(K key, GeoPosition position, V member) {
        nonNull(position, "position");
        return geoadd(key, position.longitude, position.latitude, member);
    }

    @Override
    public Uni<Boolean> geoadd(K key, GeoItem<V> item) {
        nonNull(item, "item");
        return geoadd(key, item.longitude(), item.latitude(), item.member());
    }

    @Override
    public Uni<Integer> geoadd(K key, GeoItem<V>... items) {
        return super._geoadd(key, items).map(Response::toInteger);
    }

    @Override
    public Uni<Boolean> geoadd(K key, GeoItem<V> item, GeoAddArgs args) {
        nonNull(item, "item");
        return geoadd(key, item.longitude(), item.latitude(), item.member(), args);
    }

    @Override
    public Uni<Boolean> geoadd(K key, double longitude, double latitude, V member, GeoAddArgs args) {
        return super._geoadd(key, longitude, latitude, member, args).map(r -> r.toLong() == 1L);
    }

    @Override
    public Uni<Integer> geoadd(K key, GeoAddArgs args, GeoItem<V>... items) {
        return super._geoadd(key, args, items).map(Response::toInteger);
    }

    @Override
    public Uni<Double> geodist(K key, V from, V to, GeoUnit unit) {
        return _geodist(key, from, to, unit).map(this::decodeDistance);

    }

    @Override
    public Uni<List<String>> geohash(K key, V... members) {
        return super._geohash(key, members).map(this::decodeHashList);
    }

    @Override
    public Uni<List<GeoPosition>> geopos(K key, V... members) {
        return super._geopos(key, members).map(this::decodeGeoPositions);
    }

    @Override
    public Uni<Set<V>> georadius(K key, double longitude, double latitude, double radius, GeoUnit unit) {
        return super._georadius(key, longitude, latitude, radius, unit).map(this::decodeRadiusSet);
    }

    @Override
    public Uni<List<GeoValue<V>>> georadius(K key, double longitude, double latitude, double radius, GeoUnit unit,
            GeoRadiusArgs geoArgs) {
        return super._georadius(key, longitude, latitude, radius, unit, geoArgs).map(
                r -> decodeAsListOfGeoValues(r, geoArgs.hasDistance(), geoArgs.hasCoordinates(), geoArgs.hasHash()));
    }

    @Override
    public Uni<Long> georadius(K key, double longitude, double latitude, double radius, GeoUnit unit,
            GeoRadiusStoreArgs<K> geoArgs) {
        return super._georadius(key, longitude, latitude, radius, unit, geoArgs).map(Response::toLong);
    }

    @Override
    public Uni<Set<V>> georadius(K key, GeoPosition position, double radius, GeoUnit unit) {
        nonNull(position, "position");
        return georadius(key, position.longitude, position.latitude, radius, unit);
    }

    @Override
    public Uni<List<GeoValue<V>>> georadius(K key, GeoPosition position, double radius, GeoUnit unit,
            GeoRadiusArgs geoArgs) {
        nonNull(position, "position");
        return georadius(key, position.longitude, position.latitude, radius, unit, geoArgs);
    }

    @Override
    public Uni<Long> georadius(K key, GeoPosition position, double radius, GeoUnit unit,
            GeoRadiusStoreArgs<K> geoArgs) {
        nonNull(position, "position");
        return georadius(key, position.longitude, position.latitude, radius, unit, geoArgs);
    }

    @Override
    public Uni<Set<V>> georadiusbymember(K key, V member, double distance, GeoUnit unit) {
        return super._georadiusbymember(key, member, distance, unit).map(this::decodeRadiusSet);
    }

    @Override
    public Uni<List<GeoValue<V>>> georadiusbymember(K key, V member, double distance, GeoUnit unit,
            GeoRadiusArgs geoArgs) {
        return super._georadiusbymember(key, member, distance, unit, geoArgs).map(
                r -> decodeAsListOfGeoValues(r, geoArgs.hasDistance(), geoArgs.hasCoordinates(), geoArgs.hasHash()));
    }

    @Override
    public Uni<Long> georadiusbymember(K key, V member, double distance, GeoUnit unit, GeoRadiusStoreArgs<K> geoArgs) {
        return super._georadiusbymember(key, member, distance, unit, geoArgs).map(Response::toLong);
    }

    @Override
    public Uni<List<GeoValue<V>>> geosearch(K key, GeoSearchArgs<V> geoArgs) {
        return super._geosearch(key, geoArgs).map(
                r -> decodeAsListOfGeoValues(r, geoArgs.hasDistance(), geoArgs.hasCoordinates(), geoArgs.hasHash()));
    }

    @Override
    public Uni<Long> geosearchstore(K destination, K key, GeoSearchStoreArgs<V> args, boolean storeDist) {
        return super._geosearchstore(destination, key, args, storeDist).map(Response::toLong);
    }

}
