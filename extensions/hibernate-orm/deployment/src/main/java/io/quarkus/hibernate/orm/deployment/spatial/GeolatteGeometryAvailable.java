package io.quarkus.hibernate.orm.deployment.spatial;

import java.util.function.BooleanSupplier;

/**
 * Supplier that can be used to only run build steps
 * if the Hibernate ORM extension is enabled.
 */
public class GeolatteGeometryAvailable implements BooleanSupplier {
    static final String GEOLATTE_GEOMETRY_WKB_ENCODER_CLASS = "org.geolatte.geom.codec.WkbEncoder";
    static final boolean GEOLATTE_GEOMETRY_AVAILABLE = isClassAvailable(
            GEOLATTE_GEOMETRY_WKB_ENCODER_CLASS);

    @Override
    public boolean getAsBoolean() {
        return GEOLATTE_GEOMETRY_AVAILABLE;
    }

    private static boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
