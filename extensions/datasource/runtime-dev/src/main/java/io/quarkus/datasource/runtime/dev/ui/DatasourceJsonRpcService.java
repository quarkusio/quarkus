package io.quarkus.datasource.runtime.dev.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.datasource.runtime.DatabaseSchemaProvider;

@ApplicationScoped
public class DatasourceJsonRpcService {

    public boolean reset(String ds) {
        List<DatabaseSchemaProvider> providers = new ArrayList<>();
        ServiceLoader.load(DatabaseSchemaProvider.class, Thread.currentThread().getContextClassLoader())
                .forEach(providers::add);
        for (DatabaseSchemaProvider i : providers) {
            i.resetDatabase(ds);
        }
        // Data can only be loaded once every provider is done resetting the schema
        for (DatabaseSchemaProvider i : providers) {
            i.populateDatabase(ds);
        }
        return true;
    }
}
