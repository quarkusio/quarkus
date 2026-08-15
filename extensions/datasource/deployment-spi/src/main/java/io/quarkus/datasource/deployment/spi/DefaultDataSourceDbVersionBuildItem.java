package io.quarkus.datasource.deployment.spi;

import java.util.List;
import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.common.runtime.DbVersionDefaults;

/**
 * A build item that represents the default version for a database kind.
 * Produced by database driver extensions to declare the default version
 * that should be used for dev services when no explicit version is configured.
 */
public final class DefaultDataSourceDbVersionBuildItem extends MultiBuildItem {

    private final String dbKind;
    private final String version;

    public DefaultDataSourceDbVersionBuildItem(String dbKind, String version) {
        this.dbKind = dbKind;
        this.version = version;
    }

    public String getDbKind() {
        return dbKind;
    }

    public String getVersion() {
        return version;
    }

    /**
     * Resolve the default database version for a given database kind.
     *
     * @param dbKind The database kind
     * @param defaultDbVersions List of default version build items
     * @return The default version if found, empty otherwise
     * @deprecated Use {@link #resolveDefaultDbVersion(String, List, DbVersionDefaults)} instead
     */
    @Deprecated
    public static Optional<String> resolveDefaultDbVersion(String dbKind,
            List<DefaultDataSourceDbVersionBuildItem> defaultDbVersions) {
        return resolveDefaultDbVersion(dbKind, defaultDbVersions, DbVersionDefaults.RECENT);
    }

    /**
     * Resolve the default database version for a given database kind.
     *
     * @param dbKind The database kind
     * @param defaultDbVersions List of default version build items
     * @param mode The default version mode (RECENT or COMPATIBLE)
     * @return The default version if found and mode is RECENT, empty otherwise
     */
    public static Optional<String> resolveDefaultDbVersion(String dbKind,
            List<DefaultDataSourceDbVersionBuildItem> defaultDbVersions,
            DbVersionDefaults mode) {
        if (mode == DbVersionDefaults.COMPATIBLE) {
            return Optional.empty();
        }
        if (dbKind == null) {
            return Optional.empty();
        }
        dbKind = DatabaseKind.normalize(dbKind);
        Optional<String> version = Optional.empty();
        for (var item : defaultDbVersions) {
            if (dbKind.equals(item.getDbKind())) {
                String candidate = item.getVersion();
                if (version.isPresent()) {
                    if (!version.get().equals(candidate)) {
                        throw new IllegalStateException(String.format(
                                "Found multiple default versions for db-kind '%s': %s, %s. This is a bug in relevant datasource extensions, please report it.",
                                dbKind, version.get(), candidate));
                    }
                } else {
                    version = Optional.of(item.getVersion());
                }
            }
        }
        return version;
    }
}
