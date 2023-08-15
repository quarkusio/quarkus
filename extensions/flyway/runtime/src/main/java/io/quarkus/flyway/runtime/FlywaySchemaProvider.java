package io.quarkus.flyway.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.datasource.runtime.DatabaseSchemaProvider;

public class FlywaySchemaProvider implements DatabaseSchemaProvider {

    @Override
    public void resetDatabase(String dbName) {
        for (FlywayContainer flywayContainer : Arc.container().select(FlywayContainer.class)) {
            if (flywayContainer.getDataSourceName().equals(dbName)) {
                flywayContainer.getFlyway().clean();
                flywayContainer.getFlyway().migrate();
            }
        }
    }

    @Override
    public void resetAllDatabases() {
        for (FlywayContainer flywayContainer : Arc.container().select(FlywayContainer.class)) {
            flywayContainer.getFlyway().clean();
            flywayContainer.getFlyway().migrate();
        }
    }
}
