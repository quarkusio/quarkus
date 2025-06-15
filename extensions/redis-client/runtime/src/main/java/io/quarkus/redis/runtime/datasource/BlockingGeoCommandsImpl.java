package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.geo.GeoAddArgs;
import io.quarkus.redis.datasource.geo.GeoCommands;
import io.quarkus.redis.datasource.geo.GeoItem;
import io.quarkus.redis.datasource.geo.GeoPosition;
import io.quarkus.redis.datasource.geo.GeoRadiusArgs;
import io.quarkus.redis.datasource.geo.GeoRadiusStoreArgs;
import io.quarkus.redis.datasource.geo.GeoSearchArgs;
import io.quarkus.redis.datasource.geo.GeoSearchStoreArgs;
import io.quarkus.redis.datasource.geo.GeoUnit;
import io.quarkus.redis.datasource.geo.GeoValue;
import io.quarkus.redis.datasource.geo.ReactiveGeoCommands;

public class BlockingGeoCommandsImpl<K, V> extends AbstractRedisCommandGroup implements GeoCommands<K, V> {

    private final ReactiveGeoCommands<K, V> reactive;

    public BlockingGeoCommandsImpl(RedisDataSource ds, ReactiveGeoCommands<K, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public boolean geoadd(K key, double longitude, double latitude, V member) {
        return reactive.geoadd(key, longitude, latitude, member).await().atMost(timeout);
    }

    @Override
    public boolean geoadd(K key, GeoPosition position, V member) {
        return reactive.geoadd(key, position, member).await().atMost(timeout);
    }

    @Override
    public boolean geoadd(K key, GeoItem<V> item) {
        return reactive.geoadd(key, item).await().atMost(timeout);
    }

    @Override
    public int geoadd(K key, GeoItem<V>... items) {
        return reactive.geoadd(key, items).await().atMost(timeout);
    }

    @Override
    public boolean geoadd(K key, double longitude, double latitude, V member, GeoAddArgs args) {
        return reactive.geoadd(key, longitude, latitude, member, args).await().atMost(timeout);
    }

    @Override
    public boolean geoadd(K key, GeoItem<V> item, GeoAddArgs args) {
        return reactive.geoadd(key, item, args).await().atMost(timeout);
    }

    @Override
    public int geoadd(K key, GeoAddArgs args, GeoItem<V>... items) {
        return reactive.geoadd(key, args, items).await().atMost(timeout);
    }

    @Override
    public OptionalDouble geodist(K key, V from, V to, GeoUnit unit) {
        return reactive.geodist(key, from, to, unit).map(d -> {
            if (d == null) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(d);
        }).await().atMost(timeout);
    }

    @Override
    public List<String> geohash(K key, V... members) {
        return reactive.geohash(key, members).await().atMost(timeout);
    }

    @Override
    public List<GeoPosition> geopos(K key, V... members) {
        return reactive.geopos(key, members).await().atMost(timeout);
    }

    @Override
    public Set<V> georadius(K key, double longitude, double latitude, double radius, GeoUnit unit) {
        return reactive.georadius(key, longitude, latitude, radius, unit).await().atMost(timeout);
    }

    @Override
    public Set<V> georadius(K key, GeoPosition position, double radius, GeoUnit unit) {
        return reactive.georadius(key, position, radius, unit).await().atMost(timeout);
    }

    @Override
    public List<GeoValue<V>> georadius(K key, double longitude, double latitude, double radius, GeoUnit unit,
            GeoRadiusArgs geoArgs) {
        return reactive.georadius(key, longitude, latitude, radius, unit, geoArgs).await().atMost(timeout);
    }

    @Override
    public List<GeoValue<V>> georadius(K key, GeoPosition position, double radius, GeoUnit unit,
            GeoRadiusArgs geoArgs) {
        return reactive.georadius(key, position, radius, unit, geoArgs).await().atMost(timeout);
    }

    @Override
    public long georadius(K key, double longitude, double latitude, double radius, GeoUnit unit,
            GeoRadiusStoreArgs<K> geoArgs) {
        return reactive.georadius(key, longitude, latitude, radius, unit, geoArgs).await().atMost(timeout);
    }

    @Override
    public long georadius(K key, GeoPosition position, double radius, GeoUnit unit, GeoRadiusStoreArgs<K> geoArgs) {
        return reactive.georadius(key, position, radius, unit, geoArgs).await().atMost(timeout);
    }

    @Override
    public Set<V> georadiusbymember(K key, V member, double distance, GeoUnit unit) {
        return reactive.georadiusbymember(key, member, distance, unit).await().atMost(timeout);
    }

    @Override
    public List<GeoValue<V>> georadiusbymember(K key, V member, double distance, GeoUnit unit, GeoRadiusArgs geoArgs) {
        return reactive.georadiusbymember(key, member, distance, unit, geoArgs).await().atMost(timeout);
    }

    @Override
    public long georadiusbymember(K key, V member, double distance, GeoUnit unit, GeoRadiusStoreArgs<K> geoArgs) {
        return reactive.georadiusbymember(key, member, distance, unit, geoArgs).await().atMost(timeout);
    }

    @Override
    public List<GeoValue<V>> geosearch(K key, GeoSearchArgs<V> args) {
        return reactive.geosearch(key, args).await().atMost(timeout);
    }

    @Override
    public long geosearchstore(K destination, K key, GeoSearchStoreArgs<V> args, boolean storeDist) {
        return reactive.geosearchstore(destination, key, args, storeDist).await().atMost(timeout);
    }

}
