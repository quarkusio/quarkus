package io.quarkus.flyway.mongodb.runtime;

import io.quarkus.datasource.runtime.DatabaseSchemaProvider;

/**
 * Implementation of {@link DatabaseSchemaProvider} that runs clean + migrate on each
 * active Flyway-MongoDB container. Used by Quarkus test infrastructure to reset
 * the database state between tests.
 * <p>
 * Requires {@code quarkus.flyway-mongodb.<client>.clean-disabled=false} (the default) to
 * actually run clean — set it to {@code true} in production to prevent accidental data loss.
 */
public class FlywayMongodbSchemaProvider implements DatabaseSchemaProvider {

    @Override
    public void resetDatabase(String dbName) {
        FlywayMongodbContainer container = FlywayMongodbContainerUtil.getFlywayMongodbContainer(dbName);
        if (container == null) {
            return;
        }
        container.flyway().clean();
        container.flyway().migrate();
    }

    @Override
    public void resetAllDatabases() {
        for (FlywayMongodbContainer container : FlywayMongodbContainerUtil.getActiveFlywayMongodbContainers()) {
            container.flyway().clean();
            container.flyway().migrate();
        }
    }
}
