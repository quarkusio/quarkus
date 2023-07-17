package io.quarkus.hibernate.orm.deployment.spi;

import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * An Hibernate Dialect associated with a database kind.
 */
public final class DatabaseKindDialectBuildItem extends MultiBuildItem {
    private final String dbKind;
    private final String dialect;
    private final Optional<String> defaultDatabaseProductVersion;

    /**
     * @param dbKind The DB Kind set through {@code quarkus.datasource.db-kind}
     * @param dialect The corresponding dialect to set in Hibernate ORM.
     */
    public DatabaseKindDialectBuildItem(String dbKind, String dialect) {
        this(dbKind, dialect, Optional.empty());
    }

    /**
     * @param dbKind The DB Kind set through {@code quarkus.datasource.db-kind}
     * @param dialect The corresponding dialect to set in Hibernate ORM.
     *        See {@code org.hibernate.dialect.Database} for information on how this name is resolved to a dialect.
     * @param defaultDatabaseProductVersion The default database-product-version to set in Hibernate ORM.
     *        This is useful when the default version of the dialect in Hibernate ORM
     *        is lower than what we expect in Quarkus.
     */
    public DatabaseKindDialectBuildItem(String dbKind, String dialect, String defaultDatabaseProductVersion) {
        this(dbKind, dialect, Optional.of(defaultDatabaseProductVersion));
    }

    private DatabaseKindDialectBuildItem(String dbKind, String dialect,
            Optional<String> defaultDatabaseProductVersion) {
        this.dbKind = dbKind;
        this.dialect = dialect;
        this.defaultDatabaseProductVersion = defaultDatabaseProductVersion;
    }

    public String getDbKind() {
        return dbKind;
    }

    public String getDialect() {
        return dialect;
    }

    public Optional<String> getDefaultDatabaseProductVersion() {
        return defaultDatabaseProductVersion;
    }
}
