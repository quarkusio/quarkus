package io.quarkus.hibernate.orm.runtime.service;

import static org.hibernate.internal.HEMLogging.messageLogger;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;
import org.hibernate.internal.EntityManagerMessageLogger;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.hibernate.orm.runtime.config.DialectVersions;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * A dialect factory used for runtime init;
 * simply restores the dialect used during static init.
 *
 * @see QuarkusStaticInitDialectFactory
 */
public class QuarkusRuntimeInitDialectFactory implements DialectFactory {
    private static final EntityManagerMessageLogger LOG = messageLogger(QuarkusRuntimeInitDialectFactory.class);
    private final String persistenceUnitName;
    private final boolean isFromPersistenceXml;
    private final Dialect dialect;
    private final Optional<String> datasourceName;
    private final DatabaseVersion buildTimeDbVersion;

    private boolean triedToRetrieveDbVersion = false;
    private Optional<DatabaseVersion> actualDbVersion = Optional.empty();

    public QuarkusRuntimeInitDialectFactory(String persistenceUnitName, boolean isFromPersistenceXml, Dialect dialect,
            Optional<String> datasourceName, DatabaseVersion buildTimeDbVersion) {
        this.persistenceUnitName = persistenceUnitName;
        this.isFromPersistenceXml = isFromPersistenceXml;
        this.dialect = dialect;
        this.datasourceName = datasourceName;
        this.buildTimeDbVersion = buildTimeDbVersion;
    }

    @Override
    public Dialect buildDialect(Map<String, Object> configValues, DialectResolutionInfoSource resolutionInfoSource)
            throws HibernateException {
        if (actualDbVersion.isEmpty()) {
            this.actualDbVersion = retrieveDbVersion(resolutionInfoSource);
        }
        return dialect;
    }

    public void checkActualDbVersion() {
        if (!triedToRetrieveDbVersion) {
            LOG.warnf("Persistence unit %1$s: Could not retrieve the database version to check it is at least %2$s",
                    persistenceUnitName, DialectVersions.toString(buildTimeDbVersion));
            return;
        }
        if (actualDbVersion.isPresent() && buildTimeDbVersion.isAfter(actualDbVersion.get())) {
            StringBuilder errorMessage = new StringBuilder(String.format(Locale.ROOT,
                    "Persistence unit '%1$s' was configured to run with a database version"
                            + " of at least '%2$s', but the actual version is '%3$s'."
                            + " Consider upgrading your database.",
                    persistenceUnitName,
                    DialectVersions.toString(buildTimeDbVersion), DialectVersions.toString(actualDbVersion.get())));
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
            throw new ConfigurationException(errorMessage.toString());
        }
    }

    private Optional<DatabaseVersion> retrieveDbVersion(DialectResolutionInfoSource resolutionInfoSource) {
        var databaseMetadata = resolutionInfoSource == null ? null
                : resolutionInfoSource.getDialectResolutionInfo().getDatabaseMetadata();
        if (databaseMetadata == null) {
            return Optional.empty();
        }
        try {
            triedToRetrieveDbVersion = true;
            return Optional.of(DatabaseVersion.make(databaseMetadata.getDatabaseMajorVersion(),
                    databaseMetadata.getDatabaseMinorVersion()));
        } catch (RuntimeException | SQLException e) {
            LOG.warnf(e, "Persistence unit %1$s: Could not retrieve the database version to check it is at least %2$s",
                    persistenceUnitName, buildTimeDbVersion);
            return Optional.empty();
        }
    }
}
