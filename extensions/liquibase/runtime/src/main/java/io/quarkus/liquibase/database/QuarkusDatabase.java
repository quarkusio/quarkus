package io.quarkus.liquibase.database;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import io.quarkus.liquibase.database.connection.HibernateConnection;
import io.quarkus.liquibase.database.connection.HibernateDriver;
import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;

/**
 * Base class for all Hibernate Databases. This extension interacts with Hibernate by creating standard
 * liquibase.database.Database implementations that
 * bridge what Liquibase expects and the Hibernate APIs.
 */
public class QuarkusDatabase extends AbstractJdbcDatabase implements Integrator {

    private static Metadata metadata;
    private static Dialect dialect;

    private boolean indexesForForeignKeys = false;
    public static final String DEFAULT_SCHEMA = "HIBERNATE";

    public QuarkusDatabase() {
        setDefaultCatalogName(DEFAULT_SCHEMA);
        setDefaultSchemaName(DEFAULT_SCHEMA);
    }

    @Override
    public void integrate(
            Metadata metadata,
            BootstrapContext bootstrapContext,
            SessionFactoryImplementor sessionFactory) {
        QuarkusDatabase.metadata = metadata;
        QuarkusDatabase.dialect = sessionFactory.getJdbcServices().getDialect();
    }

    @Override
    public void disintegrate(
            SessionFactoryImplementor sessionFactory,
            SessionFactoryServiceRegistry serviceRegistry) {
    }

    @Override
    public String getShortName() {
        return "hibernateQuarkus";
    }

    @Override
    protected String getDefaultDatabaseProductName() {
        return "Quarkus Hibernate";
    }

    @Override
    public boolean isCorrectDatabaseImplementation(DatabaseConnection conn) throws DatabaseException {
        return conn.getURL().startsWith("hibernate:quarkus:");
    }

    public boolean requiresPassword() {
        return false;
    }

    public boolean requiresUsername() {
        return false;
    }

    public String getDefaultDriver(String url) {
        if (url.startsWith("hibernate")) {
            return HibernateDriver.class.getName();
        }
        return null;
    }

    public int getPriority() {
        return PRIORITY_DEFAULT;
    }

    /** Returns the dialect determined during database initialization. */
    public Dialect getDialect() {
        return QuarkusDatabase.dialect;
    }

    /**
     * Return the hibernate {@link Metadata} used by this database.
     */
    public Metadata getMetadata() throws DatabaseException {
        return metadata;
    }

    /**
     * Convenience method to return the underlying HibernateConnection in the JdbcConnection returned by
     * {@link #getConnection()}
     */
    protected HibernateConnection getHibernateConnection() {
        return ((HibernateConnection) ((JdbcConnection) getConnection()).getUnderlyingConnection());
    }

    /**
     * Perform any post-configuration setting logic.
     */
    protected void afterSetup() {
        if (dialect instanceof MySQLDialect) {
            indexesForForeignKeys = true;
        }
    }

    /**
     * Returns the value of the given property. Should return the value given as a connection URL first, then fall back to
     * configuration-specific values.
     */
    public String getProperty(String name) {
        return getHibernateConnection().getProperties().getProperty(name);
    }

    @Override
    public boolean createsIndexesForForeignKeys() {
        return indexesForForeignKeys;
    }

    @Override
    public Integer getDefaultPort() {
        return 0;
    }

    @Override
    public boolean supportsInitiallyDeferrableColumns() {
        return false;
    }

    @Override
    public boolean supportsTablespaces() {
        return false;
    }

    @Override
    protected String getConnectionCatalogName() throws DatabaseException {
        return getDefaultCatalogName();
    }

    @Override
    protected String getConnectionSchemaName() {
        return getDefaultSchemaName();
    }

    @Override
    public String getDefaultSchemaName() {
        return DEFAULT_SCHEMA;
    }

    @Override
    public String getDefaultCatalogName() {
        return DEFAULT_SCHEMA;
    }

    @Override
    public boolean isSafeToRunUpdate() throws DatabaseException {
        return true;
    }

    @Override
    public boolean isCaseSensitive() {
        return false;
    }

    @Override
    public boolean supportsSchemas() {
        return true;
    }

    @Override
    public boolean supportsCatalogs() {
        return false;
    }

}
