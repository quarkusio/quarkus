package io.quarkus.reactive.datasource.deployment;

import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item for Reactive Datasources, a.k.a. Verx {@link Pool}s.
 * <p>
 * If you inject this build item when recording runtime init template calls, you are guaranteed the Pool configuration
 * has been injected. Pools are created witihin their own extensions
 * Similar to VertxPoolBuildItem, but doesn't include the Pool itself, only the name.
 *
 * Deprecated: use io/quarkus/reactive/datasource/spi/ReactiveDataSourceBuildItem.java instead
 */
@Deprecated
public final class ReactiveDataSourceBuildItem extends MultiBuildItem {

    private final String name;
    private final String dbKind;
    private final boolean isDefault;
    private final Optional<String> dbVersion;
    private final boolean dbVersionUserSpecified;

    public ReactiveDataSourceBuildItem(String name, String dbKind, boolean isDefault, Optional<String> dbVersion) {
        this(name, dbKind, isDefault, dbVersion, dbVersion.isPresent());
    }

    public ReactiveDataSourceBuildItem(String name, String dbKind, boolean isDefault, Optional<String> dbVersion,
            boolean dbVersionUserSpecified) {
        this.name = name;
        this.dbKind = dbKind;
        this.isDefault = isDefault;
        this.dbVersion = dbVersion;
        this.dbVersionUserSpecified = dbVersionUserSpecified;
    }

    public String getName() {
        return name;
    }

    public String getDbKind() {
        return dbKind;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public Optional<String> getDbVersion() {
        return dbVersion;
    }

    public boolean isDbVersionUserSpecified() {
        return dbVersionUserSpecified;
    }
}
