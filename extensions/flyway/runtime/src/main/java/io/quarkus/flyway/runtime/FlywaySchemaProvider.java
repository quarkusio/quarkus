package io.quarkus.flyway.runtime;

import io.quarkus.datasource.runtime.DatabaseSchemaProvider;

public class FlywaySchemaProvider implements DatabaseSchemaProvider {

    @Override
    public void resetDatabase(String dbName) {
        FlywayContainer flywayContainer = FlywayContainerUtil.getFlywayContainer(dbName);
        flywayContainer.getFlyway().clean();
        flywayContainer.getFlyway().migrate();
    }

    @Override
    public void resetAllDatabases() {
        for (FlywayContainer flywayContainer : FlywayContainerUtil.getActiveFlywayContainers()) {
            flywayContainer.getFlyway().clean();
            flywayContainer.getFlyway().migrate();
        }
    }
}
