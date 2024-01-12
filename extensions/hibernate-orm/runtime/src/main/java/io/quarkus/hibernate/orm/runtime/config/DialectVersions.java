package io.quarkus.hibernate.orm.runtime.config;

import org.hibernate.dialect.DatabaseVersion;

/**
 * Constants exposing the default versions of various DBs in Quarkus.
 * <p>
 * If, one day, Hibernate ORM's defaults catch up with all our default versions,
 * we could consider relying on ORM's defaults going forward.
 * <p>
 * For H2, we will probably have to keep a constant here forever,
 * as H2 is embedded and thus Quarkus determines its default version through the BOM.
 * See https://github.com/quarkusio/quarkus/issues/1886
 */
public final class DialectVersions {
    public static final class Defaults {

        // The following constants must be at least equal to the default dialect version in Hibernate ORM
        // These constants must be removed as soon as Hibernate ORM's minimum requirements become
        // greater than or equal to these versions.
        public static final String MARIADB = "10.6";
        public static final String MSSQL = "13"; // 2016

        // This must be aligned on the H2 version in the Quarkus BOM
        // This must never be removed
        public static final String H2 = "2.2.224";

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
