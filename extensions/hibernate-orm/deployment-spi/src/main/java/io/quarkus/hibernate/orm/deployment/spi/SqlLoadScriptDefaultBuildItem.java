package io.quarkus.hibernate.orm.deployment.spi;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Additional default for quarkus.hibernate-orm.data-management.init-script,
 * beyond the built-in "data.sql".
 * <p>
 * Only applies to the default persistence unit, for historical reasons.
 *
 * @deprecated "data.sql" is now a built-in default of quarkus.hibernate-orm.data-management.init-script
 *             for all persistence units, so this build item is no longer necessary.
 */
@Deprecated(since = "4.0", forRemoval = true)
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
