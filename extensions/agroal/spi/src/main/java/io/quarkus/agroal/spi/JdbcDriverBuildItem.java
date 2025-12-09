package io.quarkus.agroal.spi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Register a JDBC driver for the Agroal extension.
 * <p>
 * It allows to resolve automatically the driver from the kind, thus users don't have to set the driver anymore, except if they
 * want to use a specific one.
 */
public final class JdbcDriverBuildItem extends MultiBuildItem {

    private final String dbKind;

    private final String driverClass;

    private final Optional<String> xaDriverClass;

    private final Map<String, String> properties = new HashMap<>();

    public JdbcDriverBuildItem(String dbKind, String driverClass) {
        this(dbKind, driverClass, null, Collections.emptyMap());
    }

    public JdbcDriverBuildItem(String dbKind, String driverClass, String xaDriverClass) {
        this(dbKind, driverClass, xaDriverClass, Collections.emptyMap());
    }

    public JdbcDriverBuildItem(String dbKind, String driverClass, String xaDriverClass, Map<String, String> properties) {
        this.dbKind = dbKind;
        this.driverClass = driverClass;
        this.xaDriverClass = Optional.ofNullable(xaDriverClass);
        if (properties != null) {
            this.properties.putAll(properties);
        }
    }

    public String getDbKind() {
        return dbKind;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public Optional<String> getDriverXAClass() {
        return xaDriverClass;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
