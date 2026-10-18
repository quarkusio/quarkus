package io.quarkus.redis.datasource.geo;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

/**
 * Represents a value attached to a {@link GeoPosition}.
 */
public class GeoItem<V> {

    public final V member;
    public final GeoPosition position;

    public static <V> GeoItem<V> of(V item, double longitude, double latitude) {
        return new GeoItem<>(item, GeoPosition.of(longitude, latitude));
    }

    public static <V> GeoItem<V> of(V item, GeoPosition position) {
        return new GeoItem<>(item, position);
    }

    private GeoItem(V member, GeoPosition position) {
        this.member = member;
        this.position = nonNull(position, "position");
    }

    public V member() {
        return member;
    }

    public GeoPosition position() {
        return position;
    }

    public double longitude() {
        return position.longitude();
    }

    public double latitude() {
        return position.latitude();
    }
}
