package io.quarkus.test.db.client;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import io.quarkus.datasource.runtime.DatabaseSchemaProvider;
import io.quarkus.test.common.DevServicesContext;

public class DbTestClient implements DevServicesContext.ContextAware {

    private static final String DEFAULT_DB = "<default>";

    private DevServicesContext testContext;
    private final List<DatabaseSchemaProvider> providers = new ArrayList<>();

    public DbTestClient() {
    }

    public void resetDefaultDatabase() {
        resetDatabase(DEFAULT_DB);
    }

    public void resetDatabase(String dbName) {
        for (DatabaseSchemaProvider i : providers) {
            i.resetDatabase(dbName);
        }
    }

    public void resetAllDatabases() {
        for (DatabaseSchemaProvider i : providers) {
            i.resetAllDatabases();
        }
    }

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        this.testContext = context;

        ServiceLoader<DatabaseSchemaProvider> dbs = ServiceLoader.load(DatabaseSchemaProvider.class,
                Thread.currentThread().getContextClassLoader());
        for (DatabaseSchemaProvider i : dbs) {
            providers.add(i);
        }
    }
}
