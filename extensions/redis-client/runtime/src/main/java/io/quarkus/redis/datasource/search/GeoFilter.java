package io.quarkus.redis.datasource.search;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrBlank;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import io.quarkus.redis.datasource.geo.GeoUnit;

/**
 * Represents a geo filter
 */
public class GeoFilter {

    private final String field;
    private final double longitude;
    private final double latitude;
    private final double radius;
    private final GeoUnit unit;

    public GeoFilter(String field, double longitude, double latitude, double radius, GeoUnit unit) {
        this.field = notNullOrBlank(field, "field");
        this.longitude = longitude;
        this.latitude = latitude;
        this.radius = radius;
        this.unit = nonNull(unit, "unit");
    }

    public static GeoFilter from(String field, double longitude, double latitude, double radius, GeoUnit unit) {
        return new GeoFilter(field, longitude, latitude, radius, unit);
    }

    @Override
    public String toString() {
        return "GEOFILTER " + field + " " + longitude + " " + latitude + " " + radius + " " + unit;
    }
}
