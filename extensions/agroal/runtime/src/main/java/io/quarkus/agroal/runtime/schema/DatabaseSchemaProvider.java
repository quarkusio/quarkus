package io.quarkus.agroal.runtime.schema;

/**
 * A service interface that can be used to reset the database for dev and test mode.
 */
public interface DatabaseSchemaProvider {

    void resetDatabase(String dbName);

    void resetAllDatabases();
}
