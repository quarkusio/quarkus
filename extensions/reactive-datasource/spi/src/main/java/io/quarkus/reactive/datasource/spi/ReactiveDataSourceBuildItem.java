package io.quarkus.reactive.datasource.spi;

import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item for Reactive Datasources, a.k.a. Verx {@link Pool}s.
 * <p>
 * If you inject this build item when recording runtime init template calls, you are guaranteed the Pool configuration
 * has been injected. Pools are created witihin their own extensions
 * Similar to VertxPoolBuildItem, but doesn't include the Pool itself, only the name.
 */
public final class ReactiveDataSourceBuildItem extends MultiBuildItem {

    private final String name;
    private final String dbKind;
    private final boolean isDefault;
    private final Optional<String> version;

    public ReactiveDataSourceBuildItem(String name, String dbKind, boolean isDefault, Optional<String> version) {
        this.name = name;
        this.dbKind = dbKind;
        this.isDefault = isDefault;
        this.version = version;
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

    public Optional<String> getVersion() {
        return version;
    }
}
