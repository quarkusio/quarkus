package io.quarkus.redis.datasource.geo;

/**
 * Represents a geospatial position. The exact limits, as specified by EPSG:900913 / EPSG:3785 / OSGEO:41001 are the
 * following:
 * <ul>
 * <li>Valid longitudes are from -180 to 180 degrees.</li>
 * <li>Valid latitudes are from -85.05112878 to 85.05112878 degrees.</li>
 * </ul>
 */
public class GeoPosition {

    public final double longitude;
    public final double latitude;

    public static GeoPosition of(double longitude, double latitude) {
        return new GeoPosition(longitude, latitude);
    }

    private GeoPosition(double longitude, double latitude) {
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("The longitude must be in [-180, 180]");
        }
        if (latitude < -85.05112878 || latitude > 85.05112878) {
            throw new IllegalArgumentException("The latitude must be in [85.05112878, 85.05112878]");
        }
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public double longitude() {
        return longitude;
    }

    public double latitude() {
        return latitude;
    }
}
