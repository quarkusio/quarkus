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
 * extensions (JDBC/reactive clients) via {@code io.quarkus.datasource.deployment.spi.DefaultDataSourceDbVersionBuildItem}.
 */
public final class DialectVersions {

    /**
     * Default database versions set by Quarkus' Hibernate ORM extension for specific dialects.
     * <p>
     * These versions are applied via {@code io.quarkus.hibernate.orm.deployment.spi.DatabaseKindDialectBuildItem}
     * and serve as a last-resort fallback when the datasource extension does not provide a version.
     * <p>
     * In most cases, versions should come from datasource extensions
     * via {@code io.quarkus.datasource.deployment.spi.DefaultDataSourceDbVersionBuildItem}.
     */
    public static final class Defaults {
        /**
         * Default MariaDB version. See build-parent/pom.xml.
         */
        public static final String MARIADB = "12.1";
        /**
         * Default Microsoft SQL Server version. See build-parent/pom.xml.
         */
        public static final String MSSQL = "16";

        private Defaults() {
        }
    }

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
