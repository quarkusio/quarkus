package io.quarkus.datasource.runtime;

/**
 * A service interface that can be used to reset the database for dev and test mode.
 */
public interface DatabaseSchemaProvider {

    void resetDatabase(String dbName);

    void resetAllDatabases();
}
