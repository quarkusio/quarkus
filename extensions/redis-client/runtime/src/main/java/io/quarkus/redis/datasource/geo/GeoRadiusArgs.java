package io.quarkus.redis.datasource.geo;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

public class GeoRadiusArgs implements RedisCommandExtraArguments {

    private boolean withDistance;

    private boolean withCoordinates;

    private boolean withHash;

    private long count = -1;

    private boolean any;

    /**
     * The direction (ASC or DESC)
     */
    private String direction;

    /**
     * Use {@code ASC} order (from small to large).
     *
     * @return the current {@code GeoRadiusArgs}
     **/
    public GeoRadiusArgs ascending() {
        this.direction = "ASC";
        return this;
    }

    /**
     * Use {@code DESC} order (from large to small).
     *
     * @return the current {@code GeoRadiusArgs}
     **/
    public GeoRadiusArgs descending() {
        this.direction = "DESC";
        return this;
    }

    /**
     * Also return the distance of the returned items from the specified center. The distance is returned in the same
     * unit as the unit specified as the radius argument of the command.
     *
     * @return the current {@code GeoRadiusArgs}
     **/
    public GeoRadiusArgs withDistance() {
        this.withDistance = true;
        return this;
    }

    /**
     * Also return the longitude,latitude coordinates of the matching items.
     *
     * @return the current {@code GeoRadiusArgs}
     **/
    public GeoRadiusArgs withCoordinates() {
        this.withCoordinates = true;
        return this;
    }

    /**
     * Also return the raw geohash-encoded sorted set score of the item, in the form of a 52 bit unsigned integer. This
     * is only useful for low level hacks or debugging and is otherwise of little interest for the general user.
     *
     * @return the current {@code GeoRadiusArgs}
     **/
    public GeoRadiusArgs withHash() {
        this.withHash = true;
        return this;
    }

    /**
     * By default all the matching items are returned. It is possible to limit the results to the first N matching items
     * by using the {@code COUNT &lt;count&gt;} option.
     *
     * @param count
     *        the count value
     *
     * @return the current {@code GeoRadiusArgs}
     **/
    public GeoRadiusArgs count(long count) {
        this.count = count;
        return this;
    }

    /**
     * When ANY is provided the command will return as soon as enough matches are found, so the results may not be the
     * ones closest to the specified point, but on the other hand, the effort invested by the server is significantly
     * lower.
     * <p>
     * Using {@code ANY} requires {@code count} to be set.
     *
     * @return the current {@code GeoRadiusArgs}
     **/
    public GeoRadiusArgs any() {
        this.any = true;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        // Validation
        if (any && count == -1) {
            throw new IllegalArgumentException("ANY can only be used if COUNT is also set");
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
