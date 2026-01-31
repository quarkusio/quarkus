package io.quarkus.hibernate.orm.deployment.spatial;

import java.util.function.BooleanSupplier;

/**
 * Supplier that can be used to only run build steps
 * if Hibernate Spatial is available in the classpath.
 */
public class HibernateSpatialAvailable implements BooleanSupplier {
    static final String HIBERNATE_SPATIAL_SERVICE_CLASS = "org.hibernate.spatial.integration.SpatialService";
    static final boolean HIBERNATE_SPATIAL_AVAILABLE = isClassAvailable(
            HIBERNATE_SPATIAL_SERVICE_CLASS);

    @Override
    public boolean getAsBoolean() {
        return HIBERNATE_SPATIAL_AVAILABLE;
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
