package io.quarkus.hibernate.orm.deployment.spi;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Additional default for quarkus.hibernate-orm.sql-load-script for tests/dev, beyond the built-in "import.sql".
 * <p>
 * Only applies to the default persistence unit, for historical reasons.
 * <p>
 * Used by the Spring Data modules in particular.
 */
public final class SqlLoadScriptDefaultBuildItem extends MultiBuildItem {

    private final String resourceName;

    /**
     * @param resourceName The name of a resource in the classpath.
     */
    public SqlLoadScriptDefaultBuildItem(String resourceName) {
        Objects.requireNonNull(resourceName);
        this.resourceName = resourceName;
    }

    public String getResourceName() {
        return resourceName;
    }
}
