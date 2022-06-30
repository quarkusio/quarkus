package io.quarkus.redis.datasource.impl;

import java.time.Duration;

import io.quarkus.redis.datasource.api.geo.GeoAddArgs;
import io.quarkus.redis.datasource.api.geo.GeoItem;
import io.quarkus.redis.datasource.api.geo.GeoPosition;
import io.quarkus.redis.datasource.api.geo.GeoRadiusArgs;
import io.quarkus.redis.datasource.api.geo.GeoRadiusStoreArgs;
import io.quarkus.redis.datasource.api.geo.GeoSearchArgs;
import io.quarkus.redis.datasource.api.geo.GeoSearchStoreArgs;
import io.quarkus.redis.datasource.api.geo.GeoUnit;
import io.quarkus.redis.datasource.api.geo.ReactiveTransactionalGeoCommands;
import io.quarkus.redis.datasource.api.geo.TransactionalGeoCommands;

public class BlockingTransactionalGeoCommandsImpl<K, V> implements TransactionalGeoCommands<K, V> {

    private final ReactiveTransactionalGeoCommands<K, V> reactive;

    private final Duration timeout;

    public BlockingTransactionalGeoCommandsImpl(ReactiveTransactionalGeoCommands<K, V> reactive, Duration timeout) {
        this.reactive = reactive;
        this.timeout = timeout;
    }

    @Override
    public void geoadd(K key, double longitude, double latitude, V member) {
        this.reactive.geoadd(key, longitude, latitude, member).await().atMost(this.timeout);
    }

    @Override
    public void geoadd(K key, GeoPosition position, V member) {
        this.reactive.geoadd(key, position, member).await().atMost(this.timeout);
    }

    @Override
    public void geoadd(K key, GeoItem<V> item) {
        this.reactive.geoadd(key, item).await().atMost(this.timeout);
    }

    @Override
    public void geoadd(K key, GeoItem<V>... items) {
        this.reactive.geoadd(key, items).await().atMost(this.timeout);
    }

    @Override
    public void geoadd(K key, double longitude, double latitude, V member, GeoAddArgs args) {
        this.reactive.geoadd(key, longitude, latitude, member, args).await().atMost(this.timeout);
    }

    @Override
    public void geoadd(K key, GeoItem<V> item, GeoAddArgs args) {
        this.reactive.geoadd(key, item, args).await().atMost(this.timeout);
    }

    @Override
    public void geoadd(K key, GeoAddArgs args, GeoItem<V>... items) {
        this.reactive.geoadd(key, args, items).await().atMost(this.timeout);
    }

    @Override
    public void geodist(K key, V from, V to, GeoUnit unit) {
        this.reactive.geodist(key, from, to, unit).await().atMost(this.timeout);
    }

    @Override
    public void geohash(K key, V... members) {
        this.reactive.geohash(key, members).await().atMost(this.timeout);
    }

    @Override
    public void geopos(K key, V... members) {
        this.reactive.geopos(key, members).await().atMost(this.timeout);
    }

    @Deprecated
    @Override
    public void georadius(K key, double longitude, double latitude, double radius, GeoUnit unit) {
        this.reactive.georadius(key, longitude, latitude, radius, unit).await().atMost(this.timeout);
    }

    @Deprecated
    @Override
    public void georadius(K key, GeoPosition position, double radius, GeoUnit unit) {
        this.reactive.georadius(key, position, radius, unit).await().atMost(this.timeout);
    }

    @Deprecated
    @Override
    public void georadius(K key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusArgs geoArgs) {
        this.reactive.georadius(key, longitude, latitude, radius, unit, geoArgs).await().atMost(this.timeout);
    }

    @Deprecated
    @Override
    public void georadius(K key, GeoPosition position, double radius, GeoUnit unit, GeoRadiusArgs geoArgs) {
        this.reactive.georadius(key, position, radius, unit, geoArgs).await().atMost(this.timeout);
    }

    @Deprecated
    @Override
    public void georadius(K key, double longitude, double latitude, double radius, GeoUnit unit,
            GeoRadiusStoreArgs<K> geoArgs) {
        this.reactive.georadius(key, longitude, latitude, radius, unit, geoArgs).await().atMost(this.timeout);
    }

    @Deprecated
    @Override
    public void georadius(K key, GeoPosition position, double radius, GeoUnit unit, GeoRadiusStoreArgs<K> geoArgs) {
        this.reactive.georadius(key, position, radius, unit, geoArgs).await().atMost(this.timeout);
    }

    @Deprecated
    @Override
    public void georadiusbymember(K key, V member, double distance, GeoUnit unit) {
        this.reactive.georadiusbymember(key, member, distance, unit).await().atMost(this.timeout);
    }

    @Deprecated
    @Override
    public void georadiusbymember(K key, V member, double distance, GeoUnit unit, GeoRadiusArgs geoArgs) {
        this.reactive.georadiusbymember(key, member, distance, unit, geoArgs).await().atMost(this.timeout);
    }

    @Deprecated
    @Override
    public void georadiusbymember(K key, V member, double distance, GeoUnit unit, GeoRadiusStoreArgs<K> geoArgs) {
        this.reactive.georadiusbymember(key, member, distance, unit, geoArgs).await().atMost(this.timeout);
    }

    @Override
    public void geosearch(K key, GeoSearchArgs<V> args) {
        this.reactive.geosearch(key, args).await().atMost(this.timeout);
    }

    @Override
    public void geosearchstore(K destination, K key, GeoSearchStoreArgs<V> args, boolean storeDist) {
        this.reactive.geosearchstore(destination, key, args, storeDist).await().atMost(this.timeout);
    }
}
