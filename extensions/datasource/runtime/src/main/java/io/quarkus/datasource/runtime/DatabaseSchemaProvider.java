package io.quarkus.datasource.runtime;

/**
 * A service interface that can be used to reset the database for dev and test mode.
 */
public interface DatabaseSchemaProvider {

    void resetDatabase(String dbName);

    void resetAllDatabases();

    /**
     * Loads data into the database, once every provider had a chance to reset the schema
     * through {@link #resetDatabase(String)}.
     * <p>
     * Providers managing the schema generally don't need to implement this method,
     * since they can load data as part of the reset.
     */
    default void populateDatabase(String dbName) {
    }
}
