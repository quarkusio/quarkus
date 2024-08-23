package io.quarkus.redis.datasource.geo;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;
import io.quarkus.redis.datasource.codecs.Codec;

public class GeoSearchArgs<V> implements RedisCommandExtraArguments {

    // From member
    private V member;

    // From coordinate
    private double longitude = Double.MIN_VALUE;
    private double latitude = Double.MIN_VALUE;

    private double radius = -1;
    private double width = -1;
    private double height = -1;
    private GeoUnit unit;

    private long count = -1;
    private boolean any;

    /**
     * The direction (ASC or DESC)
     */
    private String direction;

    private boolean withDistance;

    private boolean withCoordinates;

    private boolean withHash;

    /**
     * Use the position of the given existing {@code member} in the sorted set.
     *
     * @param member the member, must not be {@code null}
     * @return the current {@code GeoSearchArgs}
     */
    public GeoSearchArgs<V> fromMember(V member) {
        if (member == null) {
            throw new IllegalArgumentException("`member` cannot be `null`");
        }
        this.member = member;
        return this;
    }

    /**
     * Use the given {@code longitude} and {@code latitude} position.
     *
     * @param longitude the longitude
     * @param latitude the latitude
     * @return the current {@code GeoSearchArgs}
     */
    public GeoSearchArgs<V> fromCoordinate(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
        return this;
    }

    /**
     * Search inside circular area according to given {@code radius}.
     *
     * @param radius the radius value
     * @param unit the unit
     * @return the current {@code GeoSearchArgs}
     **/
    public GeoSearchArgs<V> byRadius(double radius, GeoUnit unit) {
        if (radius < 0) {
            throw new IllegalArgumentException("`radius` must be positive");
        }
        if (unit == null) {
            throw new IllegalArgumentException("`unit` cannot be `null`");
        }
        this.radius = radius;
        this.unit = unit;
        return this;
    }

    /**
     * Search inside circular area according to given {@code radius}.
     *
     * @param width the width of the box
     * @param height the height of the box
     * @param unit the unit
     * @return the current {@code GeoSearchArgs}
     **/
    public GeoSearchArgs<V> byBox(double width, double height, GeoUnit unit) {
        if (width < 0) {
            throw new IllegalArgumentException("`width` must be positive");
        }
        if (height < 0) {
            throw new IllegalArgumentException("`height` must be positive");
        }
        if (unit == null) {
            throw new IllegalArgumentException("`unit` cannot be `null`");
        }
        this.width = width;
        this.height = height;
        this.unit = unit;
        return this;
    }

    /**
     * Use {@code ASC} order (from small to large).
     *
     * @return the current {@code GeoRadiusArgs}
     **/
    public GeoSearchArgs<V> ascending() {
        this.direction = "ASC";
        return this;
    }

    /**
     * Use {@code DESC} order (from large to small).
     *
     * @return the current {@code GeoRadiusArgs}
     **/
    public GeoSearchArgs<V> descending() {
        this.direction = "DESC";
        return this;
    }

    /**
     * Also return the distance of the returned items from the specified center. The distance is returned in the same
     * unit as the unit specified as the radius argument of the command.
     *
     * @return the current {@code GeoRadiusArgs}
     **/
    public GeoSearchArgs<V> withDistance() {
        this.withDistance = true;
        return this;
    }

    /**
     * Also return the longitude,latitude coordinates of the matching items.
     *
     * @return the current {@code GeoRadiusArgs}
     **/
    public GeoSearchArgs<V> withCoordinates() {
        this.withCoordinates = true;
        return this;
    }

    /**
     * Also return the raw geohash-encoded sorted set score of the item, in the form of a 52 bit unsigned integer.
     * This is only useful for low level hacks or debugging and is otherwise of little interest for the general user.
     *
     * @return the current {@code GeoRadiusArgs}
     **/
    public GeoSearchArgs<V> withHash() {
        this.withHash = true;
        return this;
    }

    /**
     * By default all the matching items are returned. It is possible to limit the results to the first N matching items
     * by using the {@code COUNT &lt;count&gt;} option.
     *
     * @param count the count value
     * @return the current {@code GeoRadiusArgs}
     **/
    public GeoSearchArgs<V> count(long count) {
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
    public GeoSearchArgs<V> any() {
        this.any = true;
        return this;
    }

    @Override
    public List<Object> toArgs(Codec codec) {
        // Validation
        if (any && count == -1) {
            throw new IllegalArgumentException("ANY can only be used if COUNT is also set");
        }

        if (radius > 0 && width > 0) {
            throw new IllegalArgumentException("BYRADIUS and BYBOX cannot be used together");
        }

        if (member != null && (latitude != Double.MIN_VALUE || longitude != Double.MIN_VALUE)) {
            throw new IllegalArgumentException("FROMMEMBER and FROMLONLAT cannot be used together");
        }

        List<Object> list = new ArrayList<>();
        if (member != null) {
            list.add("FROMMEMBER");
            list.add(new String(codec.encode(member), StandardCharsets.UTF_8));
        } else {
            list.add("FROMLONLAT");
            list.add(Double.toString(longitude));
            list.add(Double.toString(latitude));
        }

        if (radius > 0) {
            list.add("BYRADIUS");
            list.add(Double.toString(radius));
            list.add(unit.toString());
        } else {
            list.add("BYBOX");
            list.add(Double.toString(width));
            list.add(Double.toString(height));
            list.add(unit.toString());
        }

        if (direction != null) {
            list.add(direction);
        }

        if (count > 0) {
            list.add("COUNT");
            list.add(Long.toString(count));
        }

        putFlag(list, any, "ANY");

        putFlag(list, withDistance, "WITHDIST");
        putFlag(list, withCoordinates, "WITHCOORD");
        putFlag(list, withHash, "WITHHASH");

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

    private void putFlag(List<Object> list, boolean flag, String value) {
        if (flag) {
            list.add(value);
        }
    }
}
