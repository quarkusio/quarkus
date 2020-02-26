package io.quarkus.agroal.deployment;

import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Register a JDBC driver for the Agroal extension.
 * <p>
 * It allows to resolve automatically the driver from the kind, thus users don't have to set the driver anymore, except if they
 * want to use a specific one.
 */
public final class JdbcDriverBuildItem extends MultiBuildItem {

    private final String kind;

    private final String driverClass;

    private final Optional<String> xaDriverClass;

    public JdbcDriverBuildItem(String kind, String driverClass, String xaDriverClass) {
        this.kind = kind;
        this.driverClass = driverClass;
        this.xaDriverClass = Optional.ofNullable(xaDriverClass);
    }

    public JdbcDriverBuildItem(String kind, String driverClass) {
        this.kind = kind;
        this.driverClass = driverClass;
        this.xaDriverClass = Optional.empty();
    }

    public String getKind() {
        return kind;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public Optional<String> getDriverXAClass() {
        return xaDriverClass;
    }
}
