package io.quarkus.redis.datasource.geo;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

/**
 * Represents a value returned from {@code GEO} commands. The fields may not be populated. It depends on the {@code GEO}
 * commands parameters
 *
 * @param <V>
 *        the member type
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class GeoValue<V> {

    public final V member;

    public final OptionalDouble distance;

    public final OptionalLong geohash;

    public final OptionalDouble longitude;

    public final OptionalDouble latitude;

    public GeoValue(V member, OptionalDouble distance, OptionalLong geohash, OptionalDouble longitude,
            OptionalDouble latitude) {
        this.member = member;
        this.distance = distance;
        this.geohash = geohash;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public V member() {
        return member;
    }

    public OptionalDouble distance() {
        return distance;
    }

    public OptionalLong geohash() {
        return geohash;
    }

    public OptionalDouble longitude() {
        return longitude;
    }

    public OptionalDouble latitude() {
        return latitude;
    }

    public Optional<GeoPosition> position() {
        if (longitude.isPresent() && latitude.isPresent()) {
            return Optional.of(GeoPosition.of(longitude.getAsDouble(), latitude.getAsDouble()));
        }
        return Optional.empty();
    }
}
