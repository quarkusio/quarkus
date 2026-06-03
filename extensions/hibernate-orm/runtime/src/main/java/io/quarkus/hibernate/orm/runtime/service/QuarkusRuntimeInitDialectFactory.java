package io.quarkus.hibernate.orm.runtime.service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DialectLogging;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;
import org.jboss.logging.Logger;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.config.DialectVersions;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * A dialect factory used for runtime init;
 * simply restores the dialect used during static init.
 *
 * @see QuarkusStaticInitDialectFactory
 */
public class QuarkusRuntimeInitDialectFactory implements DialectFactory {
    private static final Logger LOG = DialectLogging.DIALECT_LOGGER;
    private final String persistenceUnitName;
    private final boolean isFromPersistenceXml;
    private final Dialect dialect;
    private final Optional<String> datasourceName;
    private final DatabaseVersion buildTimeDbVersion;
    private final boolean dbVersionUserSpecified;
    private final boolean versionCheckEnabled;
    private final boolean startOffline;

    private boolean triedToRetrieveDbVersion = false;
    private Optional<DatabaseVersion> actualDbVersion = Optional.empty();

    public QuarkusRuntimeInitDialectFactory(String persistenceUnitName, boolean isFromPersistenceXml, Dialect dialect,
            Optional<String> datasourceName, DatabaseVersion buildTimeDbVersion, boolean dbVersionUserSpecified,
            boolean versionCheckEnabled, boolean startOffline) {
        this.persistenceUnitName = persistenceUnitName;
        this.isFromPersistenceXml = isFromPersistenceXml;
        this.dialect = dialect;
        this.datasourceName = datasourceName;
        this.buildTimeDbVersion = buildTimeDbVersion;
        this.dbVersionUserSpecified = dbVersionUserSpecified;
        this.versionCheckEnabled = versionCheckEnabled;
        this.startOffline = startOffline;
    }

    @Override
    public Dialect buildDialect(Map<String, Object> configValues, DialectResolutionInfoSource resolutionInfoSource)
            throws HibernateException {
        if (versionCheckEnabled && actualDbVersion.isEmpty()) {
            this.actualDbVersion = retrieveDbVersion(resolutionInfoSource);
        }
        return dialect;
    }

    public void checkActualDbVersion() {
        if (!versionCheckEnabled) {
            // Warn about potentially incorrect configuration when using start-offline with default db-version
            if (startOffline && !dbVersionUserSpecified && datasourceName.isPresent()
                    && buildTimeDbVersion.getMajor() != DatabaseVersion.NO_VERSION
                    && !isEmbeddedDatabase(dialect)) {
                LOG.warnf("Persistence unit '%1$s' is configured to start offline"
                                + " but is using a default database version ('%2$s') rather than an explicitly configured one."
                                + " This may cause Hibernate ORM to behave incorrectly if the actual database version differs."
                                + " Consider setting '%3$s' explicitly to match your database version.",
                        persistenceUnitName,
                        DialectVersions.toString(buildTimeDbVersion),
                        DataSourceUtil.dataSourcePropertyKey(datasourceName.get(), "db-version"));
            }
            else {
                LOG.debugf("Persistence unit %1$s: Skipping database version check; expecting database version to be at least %2$s",
                        persistenceUnitName, DialectVersions.toString(buildTimeDbVersion));
            }
            return;
        }
        if (!triedToRetrieveDbVersion) {
            LOG.warnf("Persistence unit %1$s: Could not retrieve the database version to check it is at least %2$s",
                    persistenceUnitName, DialectVersions.toString(buildTimeDbVersion));
            return;
        }
        if (actualDbVersion.isPresent() && buildTimeDbVersion.isAfter(actualDbVersion.get())) {
            StringBuilder errorMessage = new StringBuilder();

            errorMessage.append(String.format(Locale.ROOT,
                    "Persistence unit '%1$s' was configured to run with a database version"
                            + " of at least '%2$s'%3$s, but the actual version is '%4$s'."
                            + " Consider upgrading your database.",
                    persistenceUnitName,
                    DialectVersions.toString(buildTimeDbVersion),
                    // dbVersionUserSpecified is unreliable when using persistence.xml, whose configuration is opaque to us
                    isFromPersistenceXml || dbVersionUserSpecified ? "" : " (Quarkus default)",
                    DialectVersions.toString(actualDbVersion.get())));

            // Add minimum version from dialect if available
            DatabaseVersion dialectMinVersion = dialect.getVersion();
            if (dialectMinVersion != null && dialectMinVersion.getMajor() != DatabaseVersion.NO_VERSION) {
                errorMessage.append(String.format(Locale.ROOT,
                        " The minimum version supported by the %s dialect is %s.",
                        dialect.getClass().getSimpleName(),
                        DialectVersions.toString(dialectMinVersion)));
            }
            // It shouldn't be possible to reach this code if datasourceName is not present,
            // but just let's be safe...
            if (datasourceName.isPresent()) {
                errorMessage.append(String.format(Locale.ROOT,
                        " Alternatively, rebuild your application with"
                                + " '%1$s=%2$s'"
                                + " (but this may disable some features and/or impact performance negatively).",
                        isFromPersistenceXml ? AvailableSettings.JAKARTA_HBM2DDL_DB_VERSION
                                : DataSourceUtil.dataSourcePropertyKey(datasourceName.get(), "db-version"),
                        DialectVersions.toString(actualDbVersion.get())));
            }
            if (!isFromPersistenceXml) {
                errorMessage.append(String.format(Locale.ROOT,
                        " As a last resort,"
                                + " if you are certain your application will work correctly even though the database version is incorrect,"
                                + " disable the check with"
                                + " '%1$s=false'.",
                        HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "database.version-check.enabled")));
            }
            throw new ConfigurationException(errorMessage.toString());
        }
    }

    private Optional<DatabaseVersion> retrieveDbVersion(DialectResolutionInfoSource resolutionInfoSource) {
        try {
            var resolutionInfo = resolutionInfoSource == null ? null
                    // This may throw an exception if the DB cannot be reached, in particular with Hibernate Reactive.
                    : resolutionInfoSource.getDialectResolutionInfo();
            if (resolutionInfo == null) {
                return Optional.empty();
            }
            triedToRetrieveDbVersion = true;
            return Optional.of(dialect.determineDatabaseVersion(resolutionInfo));
        } catch (RuntimeException e) {
            LOG.warnf(e, "Persistence unit %1$s: Could not retrieve the database version to check it is at least %2$s",
                    persistenceUnitName, buildTimeDbVersion);
            return Optional.empty();
        }
    }

    // Used for testing purposes
    public boolean isVersionCheckEnabled() {
        return versionCheckEnabled;
    }

    private static boolean isEmbeddedDatabase(Dialect dialect) {
        String dialectClassName = dialect.getClass().getName();
        return dialectClassName.contains("H2Dialect")
                || dialectClassName.contains("DerbyDialect")
                || dialectClassName.contains("HSQLDialect");
    }
}
