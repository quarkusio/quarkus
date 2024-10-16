package io.quarkus.redis.datasource.geo;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;
import io.quarkus.redis.datasource.codecs.Codec;

public class GeoRadiusStoreArgs<K> implements RedisCommandExtraArguments {

    private boolean withDistance;

    private boolean withCoordinates;

    private boolean withHash;

    private long count = -1;

    private boolean any;

    private String direction;

    private K storeKey;

    private K storeDistKey;

    /**
     * Use {@code ASC} order (from small to large).
     *
     * @return the current {@code GeoRadiusStoreArgs}
     **/
    public GeoRadiusStoreArgs<K> ascending() {
        this.direction = "ASC";
        return this;
    }

    /**
     * Use {@code DESC} order (from large to small).
     *
     * @return the current {@code GeoRadiusStoreArgs}
     **/
    public GeoRadiusStoreArgs<K> descending() {
        this.direction = "DESC";
        return this;
    }

    /**
     * Also return the distance of the returned items from the specified center. The distance is returned in the same
     * unit as the unit specified as the radius argument of the command.
     *
     * @return the current {@code GeoRadiusStoreArgs}
     **/
    public GeoRadiusStoreArgs<K> withDistance() {
        this.withDistance = true;
        return this;
    }

    /**
     * Also return the longitude,latitude coordinates of the matching items.
     *
     * @return the current {@code GeoRadiusStoreArgs}
     **/
    public GeoRadiusStoreArgs<K> withCoordinates() {
        this.withCoordinates = true;
        return this;
    }

    /**
     * Also return the raw geohash-encoded sorted set score of the item, in the form of a 52 bit unsigned integer.
     * This is only useful for low level hacks or debugging and is otherwise of little interest for the general user.
     *
     * @return the current {@code GeoRadiusStoreArgs}
     **/
    public GeoRadiusStoreArgs<K> withHash() {
        this.withHash = true;
        return this;
    }

    /**
     * By default, all the matching items are returned. It is possible to limit the results to the first N matching items
     * by using the {@code COUNT &lt;count&gt;} option.
     *
     * @param count the count value
     * @return the current {@code GeoRadiusStoreArgs}
     **/
    public GeoRadiusStoreArgs<K> count(long count) {
        this.count = count;
        return this;
    }

    /**
     * When ANY is provided the command will return as soon as enough matches are found, so the results may not be the
     * ones closest to the specified point, but on the other hand, the effort invested by the server is significantly
     * lower.
     *
     * Using {@code ANY} requires {@code count} to be set.
     *
     * @return the current {@code GeoRadiusStoreArgs}
     **/
    public GeoRadiusStoreArgs<K> any() {
        this.any = true;
        return this;
    }

    /**
     * Store the items in a sorted set populated with their geospatial information.
     *
     * @param storeKey the storeKey value
     * @return the current {@code GeoRadiusStoreArgs}
     **/
    public GeoRadiusStoreArgs<K> storeKey(K storeKey) {
        this.storeKey = storeKey;
        return this;
    }

    /**
     * Store the items in a sorted set populated with their distance from the center as a floating point number, in the
     * same unit specified in the radius.
     *
     * @param storeDistKey the storeDistKey value
     * @return the current {@code GeoRadiusStoreArgs}
     **/
    public GeoRadiusStoreArgs<K> storeDistKey(K storeDistKey) {
        this.storeDistKey = storeDistKey;
        return this;
    }

    @Override
    public List<Object> toArgs(Codec codec) {
        // Validation
        if (any && count == -1) {
            throw new IllegalArgumentException("ANY can only be used if COUNT is also set");
        }
        if (storeDistKey == null && storeKey == null) {
            throw new IllegalArgumentException("At least `STORE` or `STOREDIST` must be set");
        }

        List<Object> list = new ArrayList<>();
        if (withDistance) {
            list.add("WITHDIST");
        }
        if (withCoordinates) {
            list.add("WITHCOORD");
        }
        if (withHash) {
            list.add("WITHHASH");
        }

        if (count > 0) {
            list.add("COUNT");
            list.add(Long.toString(count));
        }

        if (any) {
            list.add("ANY");
        }

        list.add(direction);

        if (storeKey != null) {
            list.add("STORE");
            list.add(new String(codec.encode(storeKey), StandardCharsets.UTF_8));
        }

        if (storeDistKey != null) {
            list.add("STOREDIST");
            list.add(new String(codec.encode(storeDistKey), StandardCharsets.UTF_8));
        }

        return list;
    }

    public boolean hasDistance() {
        return withDistance;
    }

    public boolean hasHash() {
        return withHash;
    }

    public boolean hasCoordinates() {
        return withCoordinates;
    }

}
