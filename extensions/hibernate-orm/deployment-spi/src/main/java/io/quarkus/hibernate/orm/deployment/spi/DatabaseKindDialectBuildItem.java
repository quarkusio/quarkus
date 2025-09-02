package io.quarkus.hibernate.orm.deployment.spi;

import java.util.Optional;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * An Hibernate Dialect associated with a database kind.
 */
public final class DatabaseKindDialectBuildItem extends MultiBuildItem {
    private final String dbKind;
    private final Optional<String> databaseProductName;
    private final Optional<String> dialect;
    private final Set<String> matchingDialects;
    private final Optional<String> defaultDatabaseProductVersion;

    /**
     * @param dbKind The DB Kind set through {@code quarkus.datasource.db-kind}
     * @param databaseProductName The corresponding database-product-name to set in Hibernate ORM.
     *        See {@code org.hibernate.dialect.Database} for information on how this name is resolved to a dialect.
     *        Also works with {@code org.hibernate.community.dialect.CommunityDatabase} if
     *        {@code hibernate-community-dialects} is in the classpath.
     * @param dialects The corresponding dialects in Hibernate ORM,
     *        to detect the dbKind when using database multi-tenancy.
     */
    public static DatabaseKindDialectBuildItem forCoreDialect(String dbKind, String databaseProductName,
            Set<String> dialects) {
        return new DatabaseKindDialectBuildItem(dbKind, Optional.empty(), Optional.of(databaseProductName),
                dialects, Optional.empty());
    }

    /**
     * @param dbKind The DB Kind set through {@code quarkus.datasource.db-kind}
     * @param databaseProductName The corresponding database-product-name to set in Hibernate ORM.
     *        See {@code org.hibernate.dialect.Database} for information on how this name is resolved to a dialect.
     *        Also works with {@code org.hibernate.community.dialect.CommunityDatabase} if
     *        {@code hibernate-community-dialects} is in the classpath.
     * @param dialects The corresponding dialects in Hibernate ORM,
     *        to detect the dbKind when using database multi-tenancy.
     * @param defaultDatabaseProductVersion The default database-product-version to set in Hibernate ORM.
     *        This is useful when the default version of the dialect in Hibernate ORM
     *        is lower than what we expect in Quarkus.
     */
    public static DatabaseKindDialectBuildItem forCoreDialect(String dbKind, String databaseProductName,
            Set<String> dialects, String defaultDatabaseProductVersion) {
        return new DatabaseKindDialectBuildItem(dbKind, Optional.empty(), Optional.of(databaseProductName),
                dialects, Optional.of(defaultDatabaseProductVersion));
    }

    /**
     * @param dbKind The DB Kind set through {@code quarkus.datasource.db-kind}
     * @param dialect The corresponding dialect to set in Hibernate ORM.
     *        See {@code org.hibernate.dialect.Database} for information on how this name is resolved to a dialect.
     */
    public static DatabaseKindDialectBuildItem forThirdPartyDialect(String dbKind, String dialect) {
        return new DatabaseKindDialectBuildItem(dbKind, Optional.of(dialect), Optional.empty(), Set.of(dialect),
                Optional.empty());
    }

    /**
     * @param dbKind The DB Kind set through {@code quarkus.datasource.db-kind}
     * @param dialect The corresponding dialect to set in Hibernate ORM.
     *        See {@code org.hibernate.dialect.Database} for information on how this name is resolved to a dialect.
     * @param defaultDatabaseProductVersion The default database-product-version to set in Hibernate ORM.
     *        This is useful when the default version of the dialect in Hibernate ORM
     *        is lower than what we expect in Quarkus.
     */
    public static DatabaseKindDialectBuildItem forThirdPartyDialect(String dbKind, String dialect,
            String defaultDatabaseProductVersion) {
        return new DatabaseKindDialectBuildItem(dbKind, Optional.of(dialect), Optional.empty(),
                Set.of(dialect), Optional.of(defaultDatabaseProductVersion));
    }

    /**
     * @param dbKind The DB Kind set through {@code quarkus.datasource.db-kind}
     * @param dialect The corresponding dialect to set in Hibernate ORM.
     * @deprecated Use {@link #forCoreDialect(String, String, Set)}(different arguments!)
     *             for core Hibernate ORM dialects to avoid warnings on startup,
     *             or {@link #forThirdPartyDialect(String, String)} for community or third-party dialects.
     */
    @Deprecated
    public DatabaseKindDialectBuildItem(String dbKind, String dialect) {
        this(dbKind, Optional.of(dialect), Optional.empty(), Set.of(dialect), Optional.empty());
    }

    /**
     * @param dbKind The DB Kind set through {@code quarkus.datasource.db-kind}
     * @param dialect The corresponding dialect to set in Hibernate ORM.
     *        See {@code org.hibernate.dialect.Database} for information on how this name is resolved to a dialect.
     * @param defaultDatabaseProductVersion The default database-product-version to set in Hibernate ORM.
     *        This is useful when the default version of the dialect in Hibernate ORM
     *        is lower than what we expect in Quarkus.
     * @deprecated Use {@link #forCoreDialect(String, String, Set, String)}(different arguments!)
     *             for core Hibernate ORM dialects to avoid warnings on startup,
     *             or {@link #forThirdPartyDialect(String, String, String)} for community or third-party dialects.
     */
    @Deprecated
    public DatabaseKindDialectBuildItem(String dbKind, String dialect, String defaultDatabaseProductVersion) {
        this(dbKind, Optional.of(dialect), Optional.empty(), Set.of(dialect), Optional.of(defaultDatabaseProductVersion));
    }

    private DatabaseKindDialectBuildItem(String dbKind, Optional<String> dialect,
            Optional<String> databaseProductName, Set<String> matchingDialects,
            Optional<String> defaultDatabaseProductVersion) {
        this.dbKind = dbKind;
        this.dialect = dialect;
        this.matchingDialects = matchingDialects;
        this.databaseProductName = databaseProductName;
        this.defaultDatabaseProductVersion = defaultDatabaseProductVersion;
    }

    public String getDbKind() {
        return dbKind;
    }

    public String getDialect() {
        return dialect.get();
    }

    public Optional<String> getDialectOptional() {
        return dialect;
    }

    public Set<String> getMatchingDialects() {
        return matchingDialects;
    }

    public Optional<String> getDatabaseProductName() {
        return databaseProductName;
    }

    public Optional<String> getDefaultDatabaseProductVersion() {
        return defaultDatabaseProductVersion;
    }
}
