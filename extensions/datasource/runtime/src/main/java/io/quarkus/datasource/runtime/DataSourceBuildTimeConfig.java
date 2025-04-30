package io.quarkus.datasource.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface DataSourceBuildTimeConfig {

    /**
     * The kind of database we will connect to (e.g. h2, postgresql...).
     */
    Optional<@WithConverter(DatabaseKindConverter.class) String> dbKind();

    /**
     * The version of the database we will connect to (e.g. '10.0').
     *
     * CAUTION: The version number set here should follow the same numbering scheme
     * as the string returned by `java.sql.DatabaseMetaData#getDatabaseProductVersion()`
     * for your database's JDBC driver.
     * This numbering scheme may be different from the most popular one for your database;
     * for example Microsoft SQL Server 2016 would be version `13`.
     *
     * As a rule, the version set here should be as high as possible,
     * but must be lower than or equal to the version of any database your application will connect to.
     *
     * A high version will allow better performance and using more features
     * (e.g. Hibernate ORM may generate more efficient SQL,
     * avoid workarounds and take advantage of more database features),
     * but if it is higher than the version of the database you want to connect to,
     * it may lead to runtime exceptions
     * (e.g. Hibernate ORM may generate invalid SQL that your database will reject).
     *
     * Some extensions (like the Hibernate ORM extension)
     * will try to check this version against the actual database version on startup,
     * leading to a startup failure when the actual version is lower
     * or simply a warning in case the database cannot be reached.
     *
     * The default for this property is specific to each extension;
     * the Hibernate ORM extension will default to the oldest version it supports.
     *
     * @asciidoclet
     */
    Optional<@WithConverter(TrimmedStringConverter.class) String> dbVersion();

    /**
     * Dev Services.
     * <p>
     * Dev Services allow Quarkus to automatically start a database in dev and test mode.
     */
    @ConfigDocSection(generated = true)
    DevServicesBuildTimeConfig devservices();

    /**
     * Whether this particular data source should be excluded from the health check if
     * the general health check for data sources is enabled.
     * <p>
     * By default, the health check includes all configured data sources (if it is enabled).
     */
    @WithDefault("false")
    boolean healthExclude();

}
