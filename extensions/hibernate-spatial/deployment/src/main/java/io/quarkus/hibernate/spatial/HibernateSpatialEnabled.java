package io.quarkus.hibernate.spatial;

import java.util.function.BooleanSupplier;

/**
 * Supplier that can be used to only run build steps
 * if the Hibernate Envers extension is enabled.
 */
public record HibernateSpatialEnabled(HibernateSpatialBuildTimeConfig config) implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        return config.enabled();
    }

}
