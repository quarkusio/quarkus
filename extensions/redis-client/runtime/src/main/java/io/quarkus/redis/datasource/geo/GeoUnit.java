package io.quarkus.redis.datasource.geo;

import java.util.Locale;

public enum GeoUnit {

    /**
     * meter.
     */
    M,

    /**
     * kilometer.
     */
    KM,

    /**
     * feet.
     */
    FT,

    /**
     * mile.
     */
    MI;

    public String toString() {
        // Redis 6 requires lower case, uppercase has only been added in Redis 7.
        return name().toLowerCase(Locale.ROOT);
    }
}
