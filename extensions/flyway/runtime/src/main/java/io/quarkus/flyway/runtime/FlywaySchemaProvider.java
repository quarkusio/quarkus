package io.quarkus.flyway.runtime;

import io.quarkus.datasource.runtime.DatabaseSchemaProvider;

public class FlywaySchemaProvider implements DatabaseSchemaProvider {
    @Override
    public void resetDatabase(String dbName) {
        for (FlywayContainer i : FlywayRecorder.FLYWAY_CONTAINERS) {
            if (i.getDataSourceName().equals(dbName)) {
                i.getFlyway().clean();
                i.getFlyway().migrate();
            }
        }
    }

    @Override
    public void resetAllDatabases() {
        for (FlywayContainer i : FlywayRecorder.FLYWAY_CONTAINERS) {
            i.getFlyway().clean();
            i.getFlyway().migrate();
        }
    }
}
