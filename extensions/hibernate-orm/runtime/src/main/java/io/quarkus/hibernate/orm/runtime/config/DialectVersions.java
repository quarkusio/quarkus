package io.quarkus.hibernate.orm.runtime.config;

import org.hibernate.dialect.DatabaseVersion;

/**
 * Constants and utilities for database versions in Quarkus.
 * <p>
 * For H2, we keep a constant here as H2 is embedded and Quarkus determines
 * its default version through the BOM.
 * See https://github.com/quarkusio/quarkus/issues/1886
 * <p>
 * For other databases, default versions are now provided by database-specific
 * extensions (JDBC/reactive clients) via {@link io.quarkus.datasource.deployment.spi.DefaultDatabaseVersionBuildItem}.
 */
public final class DialectVersions {

    public static String toString(DatabaseVersion version) {
        StringBuilder stringBuilder = new StringBuilder();
        if (version.getMajor() != DatabaseVersion.NO_VERSION) {
            stringBuilder.append(version.getMajor());
            if (version.getMinor() != DatabaseVersion.NO_VERSION) {
                stringBuilder.append(".");
                stringBuilder.append(version.getMinor());
                if (version.getMicro() != DatabaseVersion.NO_VERSION) {
                    stringBuilder.append(".");
                    stringBuilder.append(version.getMicro());
                }
            }
        }
        return stringBuilder.toString();
    }

    private DialectVersions() {
    }

}
